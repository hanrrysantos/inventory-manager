package br.com.hanrry.inventory.notification.integration;

import br.com.hanrry.inventory.auth.security.JwtUtil;
import br.com.hanrry.inventory.inventory.batch.Batch;
import br.com.hanrry.inventory.inventory.dto.batch.BatchRequestDTO;
import br.com.hanrry.inventory.inventory.dto.batch.ConsumeBatchRequestDTO;
import br.com.hanrry.inventory.inventory.movement.LogType;
import br.com.hanrry.inventory.inventory.repository.BatchRepository;
import br.com.hanrry.inventory.inventory.service.BatchService;
import br.com.hanrry.inventory.notification.email.EmailSender;
import br.com.hanrry.inventory.shared.exception.inventory.batch.InsufficientStockException;
import br.com.hanrry.inventory.shared.exception.notification.email.EmailSendException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
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

    @MockBean
    private EmailSender emailSender;

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void authenticateOwner() {
        reset(emailSender);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("hanrry@email.com", null, List.of()));
        jdbcTemplate.update("UPDATE tb_batches SET quantity = 0 WHERE product_id = ?", PRODUCT_ID);
        jdbcTemplate.update("UPDATE tb_products SET min_stock = 20 WHERE id = ?", PRODUCT_ID);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldNotSendEmailAndShouldRollbackWhenStockIsInsufficient() {
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
        verify(emailSender, never()).sendLowStockAlert(anyList(), any());
    }

    @Test
    void shouldSendEmailAfterSuccessfulConsumeCommit() {
        String batchNumber = uniqueBatchNumber("COMMIT");
        batchService.createBatch(batchRequest(batchNumber, 10L));

        batchService.consumeStock(new ConsumeBatchRequestDTO(PRODUCT_ID, 1L));

        Batch persisted = batchRepository.findByBatchNumber(batchNumber).orElseThrow();
        assertEquals(9L, persisted.getQuantity());
        assertEquals(1, outputLogsForBatch(persisted.getId()).size());
        verify(emailSender).sendLowStockAlert(anyList(), any());
    }

    @Test
    void shouldKeepConsumedStockWhenEmailSenderFailsAfterCommit() {
        String batchNumber = uniqueBatchNumber("EMAIL-FAIL");
        batchService.createBatch(batchRequest(batchNumber, 10L));
        doThrow(new EmailSendException("Falha ao enviar alerta de estoque pelo Resend", null))
                .when(emailSender)
                .sendLowStockAlert(anyList(), any());

        batchService.consumeStock(new ConsumeBatchRequestDTO(PRODUCT_ID, 1L));

        Batch persisted = batchRepository.findByBatchNumber(batchNumber).orElseThrow();
        assertEquals(9L, persisted.getQuantity());
        assertEquals(1, outputLogsForBatch(persisted.getId()).size());
        verify(emailSender).sendLowStockAlert(anyList(), any());
    }

    @Test
    void shouldReturnNoContentWhenEmailSenderFailsAfterHttpConsume() throws Exception {
        String batchNumber = uniqueBatchNumber("HTTP-FAIL");
        batchService.createBatch(batchRequest(batchNumber, 10L));
        doThrow(new EmailSendException("Falha ao enviar alerta de estoque pelo Resend", null))
                .when(emailSender)
                .sendLowStockAlert(anyList(), any());

        mockMvc.perform(post("/api/v1/batches/consume")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtUtil.generateToken("hanrry@email.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":13,\"quantityToConsume\":1}"))
                .andExpect(status().isNoContent());

        Batch persisted = batchRepository.findByBatchNumber(batchNumber).orElseThrow();
        assertEquals(9L, persisted.getQuantity());
        verify(emailSender).sendLowStockAlert(anyList(), any());
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
