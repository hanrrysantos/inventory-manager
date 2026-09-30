package br.com.hanrry.inventory.notification.integration;

import br.com.hanrry.inventory.auth.security.JwtUtil;
import br.com.hanrry.inventory.inventory.batch.Batch;
import br.com.hanrry.inventory.inventory.dto.batch.AddStockBatchRequestDTO;
import br.com.hanrry.inventory.inventory.dto.batch.BatchRequestDTO;
import br.com.hanrry.inventory.inventory.dto.batch.ConsumeBatchRequestDTO;
import br.com.hanrry.inventory.inventory.event.RestockQueueMessage;
import br.com.hanrry.inventory.inventory.movement.LogType;
import br.com.hanrry.inventory.inventory.repository.BatchRepository;
import br.com.hanrry.inventory.inventory.service.BatchService;
import br.com.hanrry.inventory.notification.email.EmailSender;
import br.com.hanrry.inventory.notification.service.StockAlertService;
import br.com.hanrry.inventory.shared.exception.inventory.batch.InsufficientStockException;
import br.com.hanrry.inventory.shared.exception.notification.email.EmailSendException;
import br.com.hanrry.inventory.user.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static br.com.hanrry.inventory.inventory.config.RestockQueueConfig.RESTOCK_NEEDED_QUEUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class RestockNeededEventIntegrationTest {

    private static final LocalDate MANUFACTURING_DATE = LocalDate.of(2026, 1, 1);
    private static final LocalDate EXPIRY_DATE = LocalDate.of(2027, 1, 1);
    private static final Long PRODUCT_ID = 13L;
    private static final String OWNER_EMAIL = "hanrry@email.com";
    private static final long WAIT_SECONDS = 10;
    private static final long NO_MESSAGE_WAIT_MS = 2000;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static final RabbitMQContainer RABBITMQ =
            new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management-alpine"));

    @Autowired
    private BatchService batchService;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private RabbitListenerEndpointRegistry listenerRegistry;

    @SpyBean
    private StockAlertService stockAlertService;

    @MockBean
    private EmailSender emailSender;

    @DynamicPropertySource
    static void configureContainers(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);
    }

    @BeforeEach
    void authenticateOwner() {
        reset(emailSender);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(OWNER_EMAIL, null, List.of()));
        jdbcTemplate.update("UPDATE tb_batches SET quantity = 0 WHERE product_id = ?", PRODUCT_ID);
        jdbcTemplate.update("UPDATE tb_products SET min_stock = 20 WHERE id = ?", PRODUCT_ID);
    }

    @AfterEach
    void restoreQueueAndAuthentication() {
        amqpAdmin.purgeQueue(RESTOCK_NEEDED_QUEUE, false);
        listenerRegistry.getListenerContainers().forEach(MessageListenerContainer::start);
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRespondNoContentWhileEmailSenderIsStillBlocked() throws Exception {
        String batchNumber = uniqueBatchNumber("HTTP-BLOCKED");
        batchService.createBatch(batchRequest(batchNumber, 10L));
        CountDownLatch emailStarted = new CountDownLatch(1);
        CountDownLatch releaseEmail = new CountDownLatch(1);
        AtomicBoolean emailFinished = new AtomicBoolean(false);
        doAnswer(invocation -> {
            emailStarted.countDown();
            releaseEmail.await(WAIT_SECONDS, TimeUnit.SECONDS);
            emailFinished.set(true);
            return null;
        }).when(emailSender).sendLowStockAlert(anyList(), any());
        CountDownLatch alertReturned = countDownWhenQueueAlertReturns();

        performConsume().andExpect(status().isNoContent());

        assertFalse(emailFinished.get());
        Batch persisted = batchRepository.findByBatchNumber(batchNumber).orElseThrow();
        assertEquals(9L, persisted.getQuantity());
        assertEquals(1, outputLogsForBatch(persisted.getId()).size());

        assertTrue(emailStarted.await(WAIT_SECONDS, TimeUnit.SECONDS));
        releaseEmail.countDown();
        assertTrue(alertReturned.await(WAIT_SECONDS, TimeUnit.SECONDS));
        assertTrue(emailFinished.get());
        ArgumentCaptor<List<String>> productNames = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<byte[]> pdfReport = ArgumentCaptor.forClass(byte[].class);
        verify(emailSender).sendLowStockAlert(productNames.capture(), pdfReport.capture());
        assertTrue(productNames.getValue().contains(productName()));
        assertTrue(pdfReport.getValue().length > 0);
    }

    @Test
    void shouldPublishOneDurablePersistentMessageAfterConsumeCommit() throws Exception {
        stopConsumer();
        batchService.createBatch(batchRequest(uniqueBatchNumber("DURABLE"), 10L));
        Instant before = Instant.now();

        batchService.consumeStock(new ConsumeBatchRequestDTO(PRODUCT_ID, 1L));

        Instant after = Instant.now();
        awaitReadyMessages(1);
        assertEquals("true", queueDurability());
        List<Message> messages = drainQueue();
        assertEquals(1, messages.size());
        assertEquals(
                MessageDeliveryMode.PERSISTENT,
                messages.get(0).getMessageProperties().getReceivedDeliveryMode()
        );
        RestockQueueMessage body = toQueueMessage(messages.get(0));
        assertNotNull(body.eventId());
        assertEquals(PRODUCT_ID, body.productId());
        assertFalse(body.occurredAt().isBefore(before));
        assertFalse(body.occurredAt().isAfter(after));
        assertEquals(ownerId(), body.ownerId());
    }

    @Test
    void shouldKeepMessageQueuedWhileConsumerIsStoppedAndDeliverItAfterRestart() throws Exception {
        stopConsumer();
        String batchNumber = uniqueBatchNumber("STOPPED");
        batchService.createBatch(batchRequest(batchNumber, 10L));

        performConsume().andExpect(status().isNoContent());

        Batch persisted = batchRepository.findByBatchNumber(batchNumber).orElseThrow();
        assertEquals(9L, persisted.getQuantity());
        awaitReadyMessages(1);
        verify(emailSender, never()).sendLowStockAlert(anyList(), any());

        CountDownLatch alertReturned = countDownWhenQueueAlertReturns();
        listenerRegistry.getListenerContainers().forEach(MessageListenerContainer::start);

        assertTrue(alertReturned.await(WAIT_SECONDS, TimeUnit.SECONDS));
        verify(emailSender).sendLowStockAlert(anyList(), any());
        assertEquals(0, readyMessages());
    }

    @Test
    void shouldNotSendEmailWhenConsumerFindsNoLowStock() throws Exception {
        batchService.createBatch(batchRequest(uniqueBatchNumber("NO-LOW"), 10L));
        jdbcTemplate.update("UPDATE tb_products SET min_stock = -1 WHERE owner_id = ?", ownerId());
        CountDownLatch alertReturned = countDownWhenQueueAlertReturns();

        batchService.consumeStock(new ConsumeBatchRequestDTO(PRODUCT_ID, 1L));

        assertTrue(alertReturned.await(WAIT_SECONDS, TimeUnit.SECONDS));
        verify(emailSender, never()).sendLowStockAlert(anyList(), any());
    }

    @Test
    void shouldNotPublishAndShouldRollbackWhenStockIsInsufficient() {
        stopConsumer();
        String batchNumber = uniqueBatchNumber("INSUFFICIENT");
        batchService.createBatch(batchRequest(batchNumber, 5L));
        Batch created = batchRepository.findByBatchNumber(batchNumber).orElseThrow();

        assertThrows(
                InsufficientStockException.class,
                () -> batchService.consumeStock(new ConsumeBatchRequestDTO(PRODUCT_ID, 1000L))
        );

        Batch persisted = batchRepository.findByBatchNumber(batchNumber).orElseThrow();
        assertEquals(5L, persisted.getQuantity());
        assertEquals(0, outputLogsForBatch(created.getId()).size());
        assertNull(rabbitTemplate.receive(RESTOCK_NEEDED_QUEUE, NO_MESSAGE_WAIT_MS));
        verify(emailSender, never()).sendLowStockAlert(anyList(), any());
    }

    @Test
    void shouldNotPublishOnCreateBatchOrAddStock() {
        stopConsumer();
        String batchNumber = uniqueBatchNumber("CREATE-ADD");

        batchService.createBatch(batchRequest(batchNumber, 10L));
        Batch created = batchRepository.findByBatchNumber(batchNumber).orElseThrow();
        batchService.addStock(created.getId(), new AddStockBatchRequestDTO(5L));

        assertEquals(15L, batchRepository.findByBatchNumber(batchNumber).orElseThrow().getQuantity());
        assertNull(rabbitTemplate.receive(RESTOCK_NEEDED_QUEUE, NO_MESSAGE_WAIT_MS));
    }

    @Test
    void shouldPublishWhenConfirmedConsumeQuantityIsZeroOrNegative() {
        stopConsumer();

        batchService.consumeStock(new ConsumeBatchRequestDTO(PRODUCT_ID, 0L));
        batchService.consumeStock(new ConsumeBatchRequestDTO(PRODUCT_ID, -1L));

        List<Message> messages = drainQueue();
        assertEquals(2, messages.size());
        for (Message message : messages) {
            assertEquals(PRODUCT_ID, toQueueMessage(message).productId());
        }
    }

    @Test
    void shouldRunScheduledCheckInProcessWithoutPublishing() {
        stopConsumer();

        stockAlertService.checkInventoryAndNotify();

        verify(emailSender).sendLowStockAlert(anyList(), any());
        assertNull(rabbitTemplate.receive(RESTOCK_NEEDED_QUEUE, NO_MESSAGE_WAIT_MS));
    }

    @Test
    void shouldKeepStockAndNotResendWhenResendFailsAfterConsumingMessage() throws Exception {
        String batchNumber = uniqueBatchNumber("RESEND-FAIL");
        batchService.createBatch(batchRequest(batchNumber, 10L));
        doThrow(new EmailSendException("Falha ao enviar alerta de estoque pelo Resend", null))
                .when(emailSender)
                .sendLowStockAlert(anyList(), any());
        CountDownLatch alertReturned = countDownWhenQueueAlertReturns();

        performConsume().andExpect(status().isNoContent());

        assertTrue(alertReturned.await(WAIT_SECONDS, TimeUnit.SECONDS));
        stopConsumer();
        assertEquals(0, readyMessages());
        verify(emailSender, times(1)).sendLowStockAlert(anyList(), any());
        Batch persisted = batchRepository.findByBatchNumber(batchNumber).orElseThrow();
        assertEquals(9L, persisted.getQuantity());
        assertEquals(1, outputLogsForBatch(persisted.getId()).size());
    }

    private ResultActions performConsume() throws Exception {
        return mockMvc.perform(post("/api/v1/batches/consume")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtUtil.generateToken(OWNER_EMAIL))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":13,\"quantityToConsume\":1}"));
    }

    private CountDownLatch countDownWhenQueueAlertReturns() {
        CountDownLatch returned = new CountDownLatch(1);
        doAnswer(invocation -> {
            try {
                return invocation.callRealMethod();
            } finally {
                returned.countDown();
            }
        }).when(stockAlertService).checkInventoryAndNotify(any(User.class));
        return returned;
    }

    private void stopConsumer() {
        listenerRegistry.getListenerContainers().forEach(MessageListenerContainer::stop);
    }

    private int readyMessages() {
        return amqpAdmin.getQueueInfo(RESTOCK_NEEDED_QUEUE).getMessageCount();
    }

    private void awaitReadyMessages(int expected) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS);
        while (readyMessages() != expected && System.nanoTime() < deadline) {
            Thread.sleep(100);
        }
        assertEquals(expected, readyMessages());
    }

    private List<Message> drainQueue() {
        List<Message> messages = new ArrayList<>();
        Message message = rabbitTemplate.receive(RESTOCK_NEEDED_QUEUE, TimeUnit.SECONDS.toMillis(WAIT_SECONDS));
        while (message != null) {
            messages.add(message);
            message = rabbitTemplate.receive(RESTOCK_NEEDED_QUEUE, NO_MESSAGE_WAIT_MS);
        }
        return messages;
    }

    private RestockQueueMessage toQueueMessage(Message message) {
        return (RestockQueueMessage) rabbitTemplate.getMessageConverter().fromMessage(message);
    }

    private String queueDurability() throws Exception {
        var result = RABBITMQ.execInContainer(
                "rabbitmqctl", "list_queues", "--quiet", "--no-table-headers", "name", "durable");
        return result.getStdout().lines()
                .map(line -> line.trim().split("\\s+"))
                .filter(columns -> columns[0].equals(RESTOCK_NEEDED_QUEUE))
                .map(columns -> columns[1])
                .findFirst()
                .orElseThrow();
    }

    private Long ownerId() {
        return jdbcTemplate.queryForObject("SELECT id FROM tb_users WHERE email = ?", Long.class, OWNER_EMAIL);
    }

    private String productName() {
        return jdbcTemplate.queryForObject("SELECT name FROM tb_products WHERE id = ?", String.class, PRODUCT_ID);
    }

    private BatchRequestDTO batchRequest(String batchNumber, long quantity) {
        return new BatchRequestDTO(
                batchNumber,
                quantity,
                MANUFACTURING_DATE,
                EXPIRY_DATE,
                BigDecimal.valueOf(10.00),
                PRODUCT_ID
        );
    }

    private String uniqueBatchNumber(String prefix) {
        return prefix + "-" + System.nanoTime();
    }

    private List<Map<String, Object>> outputLogsForBatch(Long batchId) {
        return jdbcTemplate.queryForList(
                "SELECT type, quantity FROM tb_inventory_logs WHERE batch_id = ? AND type = ?",
                batchId,
                LogType.OUTPUT.name()
        );
    }
}
