package br.com.hanrry.inventory.notification.integration;

import br.com.hanrry.inventory.inventory.event.RestockQueueMessage;
import br.com.hanrry.inventory.notification.email.EmailSender;
import br.com.hanrry.inventory.shared.exception.notification.email.EmailSendException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static br.com.hanrry.inventory.inventory.config.RestockQueueConfig.RESTOCK_NEEDED_DLQ;
import static br.com.hanrry.inventory.inventory.config.RestockQueueConfig.RESTOCK_NEEDED_QUEUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class RestockNotificationResilienceIntegrationTest {

    private static final UUID EVENT_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final String OWNER_EMAIL = "hanrry@email.com";
    private static final long PRODUCT_ID = 13L;
    private static final long WAIT_MS = 15_000;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static final RabbitMQContainer RABBITMQ =
            new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management-alpine"));

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
        registry.add("spring.rabbitmq.listener.simple.retry.max-attempts", () -> 3);
    }

    @BeforeEach
    void prepareLowStockScenario() {
        reset(emailSender);
        jdbcTemplate.update("UPDATE tb_products SET min_stock = 20 WHERE id = ?", PRODUCT_ID);
        jdbcTemplate.update("UPDATE tb_batches SET quantity = 5 WHERE product_id = ?", PRODUCT_ID);
        purgeQueues();
        jdbcTemplate.update("DELETE FROM tb_restock_notifications");
    }

    private long ownerId() {
        return jdbcTemplate.queryForObject("SELECT id FROM tb_users WHERE email = ?", Long.class, OWNER_EMAIL);
    }

    @AfterEach
    void purgeQueues() {
        amqpAdmin.purgeQueue(RESTOCK_NEEDED_QUEUE, false);
        amqpAdmin.purgeQueue(RESTOCK_NEEDED_DLQ, false);
    }

    @Test
    void shouldSendOnlyOneEmailWhenSameMessageIsDeliveredTwice() throws InterruptedException {
        RestockQueueMessage message = new RestockQueueMessage(
                EVENT_ID,
                PRODUCT_ID,
                Instant.parse("2026-09-29T12:00:00Z"),
                ownerId()
        );

        rabbitTemplate.convertAndSend(RESTOCK_NEEDED_QUEUE, message);
        awaitSentStatus();
        verify(emailSender, atLeastOnce()).sendLowStockAlert(anyList(), any());
        reset(emailSender);

        rabbitTemplate.convertAndSend(RESTOCK_NEEDED_QUEUE, message);
        Thread.sleep(2_000);

        verify(emailSender, times(0)).sendLowStockAlert(anyList(), any());
        assertEquals(1, countNotifications(EVENT_ID, "SENT"));
    }

    @Test
    void shouldRetryUntilSuccessAndMarkSent() throws InterruptedException {
        AtomicInteger attempts = new AtomicInteger();
        doAnswer(invocation -> {
            if (attempts.incrementAndGet() < 3) {
                throw new EmailSendException("temporary", null);
            }
            return null;
        }).when(emailSender).sendLowStockAlert(anyList(), any());

        publishDefaultMessage();
        awaitSentStatus();

        assertEquals(3, attempts.get());
        assertEquals(1, countNotifications(EVENT_ID, "SENT"));
        assertEquals(0, readyMessages(RESTOCK_NEEDED_DLQ));
    }

    @Test
    void shouldMoveToDlqAndMarkFailedAfterMaxAttempts() throws InterruptedException {
        doThrow(new EmailSendException("permanent", null))
                .when(emailSender)
                .sendLowStockAlert(anyList(), any());

        publishDefaultMessage();
        awaitFailedStatus();
        awaitDlqMessage();

        verify(emailSender, times(3)).sendLowStockAlert(anyList(), any());
        assertEquals(1, countNotifications(EVENT_ID, "FAILED"));
        assertEquals(0, readyMessages(RESTOCK_NEEDED_QUEUE));
        assertEquals(1, readyMessages(RESTOCK_NEEDED_DLQ));
    }

    private void publishDefaultMessage() {
        rabbitTemplate.convertAndSend(
                RESTOCK_NEEDED_QUEUE,
                new RestockQueueMessage(EVENT_ID, PRODUCT_ID, Instant.parse("2026-09-29T12:00:00Z"), ownerId())
        );
    }

    private void awaitSentStatus() throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(WAIT_MS);
        while (System.nanoTime() < deadline) {
            if (countNotifications(EVENT_ID, "SENT") == 1) {
                return;
            }
            Thread.sleep(200);
        }
        assertEquals(1, countNotifications(EVENT_ID, "SENT"));
    }

    private void awaitFailedStatus() throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(WAIT_MS);
        while (System.nanoTime() < deadline) {
            if (countNotifications(EVENT_ID, "FAILED") == 1) {
                return;
            }
            Thread.sleep(200);
        }
        assertEquals(1, countNotifications(EVENT_ID, "FAILED"));
    }

    private void awaitDlqMessage() throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(WAIT_MS);
        while (System.nanoTime() < deadline) {
            if (readyMessages(RESTOCK_NEEDED_DLQ) >= 1) {
                return;
            }
            Thread.sleep(200);
        }
        assertEquals(1, readyMessages(RESTOCK_NEEDED_DLQ));
    }

    private int countNotifications(UUID eventId, String status) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_restock_notifications WHERE event_id = ?::uuid AND status = ?",
                Integer.class,
                eventId.toString(),
                status
        );
        return count == null ? 0 : count;
    }

    private int readyMessages(String queue) {
        return amqpAdmin.getQueueInfo(queue).getMessageCount();
    }
}
