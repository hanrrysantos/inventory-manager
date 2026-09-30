package br.com.hanrry.inventory.notification.integration;

import br.com.hanrry.inventory.auth.security.JwtUtil;
import br.com.hanrry.inventory.inventory.batch.Batch;
import br.com.hanrry.inventory.inventory.dto.batch.BatchRequestDTO;
import br.com.hanrry.inventory.inventory.movement.LogType;
import br.com.hanrry.inventory.inventory.repository.BatchRepository;
import br.com.hanrry.inventory.inventory.service.BatchService;
import br.com.hanrry.inventory.notification.email.EmailSender;
import br.com.hanrry.inventory.notification.service.StockAlertService;
import br.com.hanrry.inventory.user.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class RestockPublishFailureIntegrationTest {

    private static final LocalDate MANUFACTURING_DATE = LocalDate.of(2026, 1, 1);
    private static final LocalDate EXPIRY_DATE = LocalDate.of(2027, 1, 1);
    private static final Long PRODUCT_ID = 13L;
    private static final String OWNER_EMAIL = "hanrry@email.com";
    private static final long NO_DELIVERY_WAIT_MS = 2000;
    private static final int CLOSED_PORT = closedPort();
    private static final Pattern PUBLISH_FAILURE_LOG = Pattern.compile(
            "Falha ao publicar reposição na fila após commit\\. eventId=[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

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

    @SpyBean
    private StockAlertService stockAlertService;

    @MockBean
    private EmailSender emailSender;

    @DynamicPropertySource
    static void configureContainers(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.rabbitmq.host", () -> "localhost");
        registry.add("spring.rabbitmq.port", () -> CLOSED_PORT);
    }

    @BeforeEach
    void authenticateOwner() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(OWNER_EMAIL, null, List.of()));
        jdbcTemplate.update("UPDATE tb_batches SET quantity = 0 WHERE product_id = ?", PRODUCT_ID);
        jdbcTemplate.update("UPDATE tb_products SET min_stock = 20 WHERE id = ?", PRODUCT_ID);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRespondNoContentAndKeepStockWhenBrokerIsUnreachable(CapturedOutput output) throws Exception {
        String batchNumber = "BROKER-DOWN-" + System.nanoTime();
        batchService.createBatch(new BatchRequestDTO(
                batchNumber,
                10L,
                MANUFACTURING_DATE,
                EXPIRY_DATE,
                BigDecimal.valueOf(10.00),
                PRODUCT_ID
        ));

        mockMvc.perform(post("/api/v1/batches/consume")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtUtil.generateToken(OWNER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":13,\"quantityToConsume\":1}"))
                .andExpect(status().isNoContent());

        Batch persisted = batchRepository.findByBatchNumber(batchNumber).orElseThrow();
        assertEquals(9L, persisted.getQuantity());
        assertEquals(1, jdbcTemplate.queryForList(
                "SELECT id FROM tb_inventory_logs WHERE batch_id = ? AND type = ?",
                persisted.getId(),
                LogType.OUTPUT.name()
        ).size());
        assertTrue(PUBLISH_FAILURE_LOG.matcher(output.getAll()).find());
        verify(stockAlertService, after(NO_DELIVERY_WAIT_MS).never()).checkInventoryAndNotify(any(User.class));
        verify(emailSender, never()).sendLowStockAlert(anyList(), any());
    }

    private static int closedPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
