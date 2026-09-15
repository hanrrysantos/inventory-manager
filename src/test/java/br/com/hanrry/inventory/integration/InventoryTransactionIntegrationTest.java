package br.com.hanrry.inventory.integration;

import br.com.hanrry.inventory.dto.batch.BatchRequestDTO;
import br.com.hanrry.inventory.dto.batch.BatchResponseDTO;
import br.com.hanrry.inventory.dto.batch.ConsumeBatchRequestDTO;
import br.com.hanrry.inventory.entity.Batch;
import br.com.hanrry.inventory.entity.enums.LogType;
import br.com.hanrry.inventory.exception.batch.InsufficientStockException;
import br.com.hanrry.inventory.repository.BatchRepository;
import br.com.hanrry.inventory.service.BatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class InventoryTransactionIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final LocalDate MANUFACTURING_DATE = LocalDate.of(2026, 1, 1);
    private static final LocalDate EXPIRY_DATE = LocalDate.of(2027, 1, 1);

    @Autowired
    private BatchService batchService;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void disableSeededLowStockAlerts() {
        jdbcTemplate.update("UPDATE tb_products SET min_stock = 0");
    }

    @Test
    void shouldCreateBatchAndPersistInputLogInSameTransaction() {
        String batchNumber = uniqueBatchNumber("CREATE");
        BatchResponseDTO response = batchService.createBatch(batchRequest(batchNumber, 10L));

        Batch persistedBatch = batchRepository.findByBatchNumber(batchNumber).orElseThrow();
        List<Map<String, Object>> logs = logsForBatch(persistedBatch.getId());

        assertEquals(response.id(), persistedBatch.getId());
        assertEquals(10L, persistedBatch.getQuantity());
        assertEquals(1, logs.size());
        assertEquals("INPUT", logs.getFirst().get("type"));
        assertEquals(10L, logs.getFirst().get("quantity"));
        assertEquals(persistedBatch.getProduct().getId(), logs.getFirst().get("product_id"));
    }

    @Test
    void shouldConsumeRealStockInExpiryOrderAndPersistOutputLogs() {
        String olderBatchNumber = uniqueBatchNumber("OLDER");
        String newerBatchNumber = uniqueBatchNumber("NEWER");
        batchService.createBatch(batchRequest(olderBatchNumber, 5L, LocalDate.of(2026, 5, 1)));
        batchService.createBatch(batchRequest(newerBatchNumber, 10L, LocalDate.of(2026, 6, 1)));

        batchService.consumeStock(new ConsumeBatchRequestDTO(1L, 7L));

        Batch olderBatch = batchRepository.findByBatchNumber(olderBatchNumber).orElseThrow();
        Batch newerBatch = batchRepository.findByBatchNumber(newerBatchNumber).orElseThrow();

        assertEquals(0L, olderBatch.getQuantity());
        assertEquals(8L, newerBatch.getQuantity());
        assertEquals(1, outputLogsForBatch(olderBatch.getId()).size());
        assertEquals(5L, outputLogsForBatch(olderBatch.getId()).getFirst().get("quantity"));
        assertEquals(1, outputLogsForBatch(newerBatch.getId()).size());
        assertEquals(2L, outputLogsForBatch(newerBatch.getId()).getFirst().get("quantity"));
    }

    @Test
    void shouldRollbackBatchChangesAndLogsWhenStockIsInsufficient() {
        String batchNumber = uniqueBatchNumber("ROLLBACK");
        batchService.createBatch(batchRequest(batchNumber, 5L));
        Batch persistedBeforeConsumption = batchRepository.findByBatchNumber(batchNumber).orElseThrow();
        long logsBeforeConsumption = outputLogsForBatch(persistedBeforeConsumption.getId()).size();

        assertThrows(
                InsufficientStockException.class,
                () -> batchService.consumeStock(new ConsumeBatchRequestDTO(1L, 1000L))
        );

        Batch persistedAfterConsumption = batchRepository.findByBatchNumber(batchNumber).orElseThrow();
        assertEquals(5L, persistedAfterConsumption.getQuantity());
        assertEquals(logsBeforeConsumption, outputLogsForBatch(persistedAfterConsumption.getId()).size());
        assertNotNull(persistedAfterConsumption.getProduct());
    }

    private BatchRequestDTO batchRequest(String batchNumber, long quantity) {
        return batchRequest(batchNumber, quantity, EXPIRY_DATE);
    }

    private BatchRequestDTO batchRequest(String batchNumber, long quantity, LocalDate expiryDate) {
        return new BatchRequestDTO(
                batchNumber,
                quantity,
                MANUFACTURING_DATE,
                expiryDate,
                BigDecimal.valueOf(10.00),
                1L
        );
    }

    private String uniqueBatchNumber(String prefix) {
        return prefix + "-" + System.nanoTime();
    }

    private List<Map<String, Object>> logsForBatch(Long batchId) {
        return jdbcTemplate.queryForList(
                "SELECT type, quantity, product_id FROM tb_inventory_logs WHERE batch_id = ?",
                batchId
        );
    }

    private List<Map<String, Object>> outputLogsForBatch(Long batchId) {
        return logsForBatch(batchId).stream()
                .filter(log -> LogType.OUTPUT.name().equals(log.get("type")))
                .toList();
    }
}
