package br.com.hanrry.inventory.notification.integration;

import br.com.hanrry.inventory.auth.security.JwtUtil;
import br.com.hanrry.inventory.inventory.dto.batch.AddStockBatchRequestDTO;
import br.com.hanrry.inventory.inventory.dto.batch.BatchRequestDTO;
import br.com.hanrry.inventory.inventory.dto.batch.ConsumeBatchRequestDTO;
import br.com.hanrry.inventory.inventory.event.RestockQueueMessage;
import br.com.hanrry.inventory.inventory.service.BatchService;
import br.com.hanrry.inventory.notification.email.EmailSender;
import br.com.hanrry.inventory.notification.service.StockAlertService;
import br.com.hanrry.inventory.shared.exception.inventory.batch.InsufficientStockException;
import br.com.hanrry.inventory.shared.exception.notification.email.EmailSendException;
import br.com.hanrry.inventory.user.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static br.com.hanrry.inventory.inventory.config.RestockQueueConfig.RESTOCK_NEEDED_DLQ;
import static br.com.hanrry.inventory.inventory.config.RestockQueueConfig.RESTOCK_NEEDED_QUEUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "management.server.port=0"
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class ObservabilityIntegrationTest {

    private static final LocalDate MANUFACTURING_DATE = LocalDate.of(2026, 1, 1);
    private static final LocalDate EXPIRY_DATE = LocalDate.of(2027, 1, 1);
    private static final Long PRODUCT_ID = 13L;
    private static final String OWNER_EMAIL = "hanrry@email.com";
    private static final long WAIT_SECONDS = 10;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static final RabbitMQContainer RABBITMQ =
            new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management-alpine"));

    @LocalManagementPort
    int managementPort;

    @Autowired
    private TestRestTemplate restTemplate;

    private final RestTemplate rawRestTemplate = restTemplateWithoutErrorHandler();

    @Autowired
    private BatchService batchService;

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
        registry.add("management.server.port", () -> 0);
    }

    @BeforeEach
    void authenticateOwner() throws InterruptedException {
        listenerRegistry.getListenerContainers().forEach(MessageListenerContainer::stop);
        amqpAdmin.purgeQueue(RESTOCK_NEEDED_QUEUE, false);
        amqpAdmin.purgeQueue(RESTOCK_NEEDED_DLQ, false);
        jdbcTemplate.update("DELETE FROM tb_restock_notifications");
        awaitEmptyQueue();
        listenerRegistry.getListenerContainers().forEach(MessageListenerContainer::start);
        reset(emailSender);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(OWNER_EMAIL, null, List.of()));
        jdbcTemplate.update("UPDATE tb_batches SET quantity = 0 WHERE product_id = ?", PRODUCT_ID);
        jdbcTemplate.update("UPDATE tb_products SET min_stock = 20 WHERE id = ?", PRODUCT_ID);
    }

    @AfterEach
    void restore() {
        amqpAdmin.purgeQueue(RESTOCK_NEEDED_QUEUE, false);
        amqpAdmin.purgeQueue(RESTOCK_NEEDED_DLQ, false);
        jdbcTemplate.update("DELETE FROM tb_restock_notifications");
        listenerRegistry.getListenerContainers().forEach(MessageListenerContainer::start);
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldIncreaseRestockCountersAndReturnRequestIdOnConfirmedConsume() throws Exception {
        double consumptionBefore = counter("stock_consumption_total");
        double publishedBefore = counter("restock_events_total");
        double sentBefore = counter("notifications_sent_total");
        batchService.createBatch(batchRequest(uniqueBatchNumber("OBS-OK"), 10L));
        CountDownLatch alertReturned = countDownWhenQueueAlertReturns();

        MvcResult result = mockMvc.perform(post("/api/v1/batches/consume")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtUtil.generateToken(OWNER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":13,\"quantityToConsume\":1}"))
                .andExpect(status().isNoContent())
                .andExpect(header().exists("X-Request-Id"))
                .andReturn();

        UUID.fromString(result.getResponse().getHeader("X-Request-Id"));
        assertTrue(alertReturned.await(WAIT_SECONDS, TimeUnit.SECONDS));
        awaitStatusCount("SENT", 1);

        assertEquals(consumptionBefore + 1, counter("stock_consumption_total"), 0.001);
        assertEquals(publishedBefore + 1, counter("restock_events_total"), 0.001);
        assertEquals(sentBefore + 1, counter("notifications_sent_total"), 0.001);
        assertTrue(scrape().contains("jvm_memory"));
    }

    @Test
    void shouldNotIncreaseSentAgainOnSentRedelivery() throws Exception {
        batchService.createBatch(batchRequest(uniqueBatchNumber("OBS-REDELIVER"), 10L));
        CountDownLatch first = countDownWhenQueueAlertReturns();
        batchService.consumeStock(new ConsumeBatchRequestDTO(PRODUCT_ID, 1L));
        assertTrue(first.await(WAIT_SECONDS, TimeUnit.SECONDS));
        awaitStatusCount("SENT", 1);
        double sentAfterFirst = counter("notifications_sent_total");

        UUID eventId = jdbcTemplate.queryForObject(
                "SELECT event_id FROM tb_restock_notifications",
                UUID.class
        );
        rabbitTemplate.convertAndSend(RESTOCK_NEEDED_QUEUE, new RestockQueueMessage(
                eventId,
                PRODUCT_ID,
                Instant.now(),
                ownerId()
        ));
        awaitEmptyQueue();
        Thread.sleep(500);

        assertEquals(sentAfterFirst, counter("notifications_sent_total"), 0.001);
        assertEquals(1, statusCount("SENT"));
    }

    @Test
    void shouldNotIncreaseDomainCountersOnRollbackJobOrStockEntry() throws Exception {
        double consumption = counter("stock_consumption_total");
        double published = counter("restock_events_total");
        double sent = counter("notifications_sent_total");
        double failed = counter("notifications_failed_total");

        String batchNumber = uniqueBatchNumber("OBS-NO");
        batchService.createBatch(batchRequest(batchNumber, 5L));
        var created = jdbcTemplate.queryForObject(
                "SELECT id FROM tb_batches WHERE batch_number = ?",
                Long.class,
                batchNumber
        );
        batchService.addStock(created, new AddStockBatchRequestDTO(5L));
        assertThrows(
                InsufficientStockException.class,
                () -> batchService.consumeStock(new ConsumeBatchRequestDTO(PRODUCT_ID, 1000L))
        );
        clearInvocations(emailSender);
        stockAlertService.checkInventoryAndNotify();

        assertEquals(consumption, counter("stock_consumption_total"), 0.001);
        assertEquals(published, counter("restock_events_total"), 0.001);
        assertEquals(sent, counter("notifications_sent_total"), 0.001);
        assertEquals(failed, counter("notifications_failed_total"), 0.001);
    }

    @Test
    void shouldIncreaseFailedWhenOwnerDoesNotExist() throws Exception {
        double failedBefore = counter("notifications_failed_total");
        rabbitTemplate.convertAndSend(RESTOCK_NEEDED_QUEUE, new RestockQueueMessage(
                UUID.randomUUID(),
                PRODUCT_ID,
                Instant.now(),
                9_999_999L
        ));
        awaitCounter("notifications_failed_total", failedBefore + 1);
        assertEquals(failedBefore + 1, counter("notifications_failed_total"), 0.001);
    }

    @Test
    void shouldIncreaseSentOnceWhenEmailFailsThenSucceeds() throws Exception {
        double sentBefore = counter("notifications_sent_total");
        double failedBefore = counter("notifications_failed_total");
        batchService.createBatch(batchRequest(uniqueBatchNumber("OBS-RETRY"), 10L));
        CountDownLatch alertReturned = countDownWhenQueueAlertReturns();
        doThrow(new EmailSendException("transient", null))
                .doAnswer(invocation -> null)
                .when(emailSender)
                .sendLowStockAlert(anyList(), any());

        batchService.consumeStock(new ConsumeBatchRequestDTO(PRODUCT_ID, 1L));

        assertTrue(alertReturned.await(WAIT_SECONDS, TimeUnit.SECONDS));
        awaitStatusCount("SENT", 1);
        assertEquals(sentBefore + 1, counter("notifications_sent_total"), 0.001);
        assertEquals(failedBefore, counter("notifications_failed_total"), 0.001);
    }

    private double counter(String prometheusName) {
        Matcher matcher = Pattern.compile(
                "^" + Pattern.quote(prometheusName) + "(?:\\{[^}]*\\})?\\s+([0-9.eE+-]+)",
                Pattern.MULTILINE
        ).matcher(scrape());
        if (!matcher.find()) {
            return 0.0;
        }
        return Double.parseDouble(matcher.group(1));
    }

    private String scrape() {
        ResponseEntity<String> response = rawRestTemplate.getForEntity(
                "http://localhost:" + managementPort + "/actuator/prometheus",
                String.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        return response.getBody();
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

    private void awaitStatusCount(String status, int expected) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS);
        while (System.nanoTime() < deadline) {
            if (statusCount(status) == expected) {
                return;
            }
            Thread.sleep(100);
        }
        assertEquals(expected, statusCount(status));
    }

    private void awaitCounter(String prometheusName, double expected) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS);
        while (System.nanoTime() < deadline) {
            if (Math.abs(counter(prometheusName) - expected) < 0.001) {
                return;
            }
            Thread.sleep(100);
        }
        assertEquals(expected, counter(prometheusName), 0.001);
    }

    private int statusCount(String status) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_restock_notifications WHERE status = ?",
                Integer.class,
                status
        );
        return count == null ? 0 : count;
    }

    private void awaitEmptyQueue() throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS);
        while (System.nanoTime() < deadline) {
            if (amqpAdmin.getQueueInfo(RESTOCK_NEEDED_QUEUE).getMessageCount() == 0) {
                Thread.sleep(200);
                if (amqpAdmin.getQueueInfo(RESTOCK_NEEDED_QUEUE).getMessageCount() == 0) {
                    return;
                }
            }
            Thread.sleep(100);
        }
    }

    private Long ownerId() {
        return jdbcTemplate.queryForObject("SELECT id FROM tb_users WHERE email = ?", Long.class, OWNER_EMAIL);
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

    private static RestTemplate restTemplateWithoutErrorHandler() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }
        });
        return restTemplate;
    }
}
