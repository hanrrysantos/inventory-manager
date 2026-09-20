package br.com.hanrry.inventory.inventory.integration;

import br.com.hanrry.inventory.inventory.dto.batch.BatchRequestDTO;
import br.com.hanrry.inventory.inventory.dto.batch.AddStockBatchRequestDTO;
import br.com.hanrry.inventory.inventory.dto.batch.BatchResponseDTO;
import br.com.hanrry.inventory.inventory.dto.batch.ConsumeBatchRequestDTO;
import br.com.hanrry.inventory.inventory.batch.Batch;
import br.com.hanrry.inventory.inventory.movement.LogType;
import br.com.hanrry.inventory.shared.exception.inventory.batch.InsufficientStockException;
import br.com.hanrry.inventory.inventory.repository.BatchRepository;
import br.com.hanrry.inventory.inventory.service.BatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("hanrry@email.com", null, List.of()));
        jdbcTemplate.update("UPDATE tb_products SET min_stock = 0");
    }

    @org.junit.jupiter.api.AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
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
        jdbcTemplate.update("UPDATE tb_batches SET quantity = 0 WHERE product_id = 13");
        batchService.createBatch(batchRequest(olderBatchNumber, 5L, LocalDate.of(2027, 5, 1), 13L));
        batchService.createBatch(batchRequest(newerBatchNumber, 10L, LocalDate.of(2027, 6, 1), 13L));

        batchService.consumeStock(new ConsumeBatchRequestDTO(13L, 7L));

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
    void shouldPersistAddStockAndInputLogInSameTransaction() {
        String batchNumber = uniqueBatchNumber("ADD");
        batchService.createBatch(batchRequest(batchNumber, 10L, EXPIRY_DATE, 2L));
        Batch createdBatch = batchRepository.findByBatchNumber(batchNumber).orElseThrow();

        batchService.addStock(createdBatch.getId(), new AddStockBatchRequestDTO(5L));

        Batch persistedBatch = batchRepository.findByBatchNumber(batchNumber).orElseThrow();
        List<Map<String, Object>> inputLogs = logsForBatch(persistedBatch.getId()).stream()
                .filter(log -> "INPUT".equals(log.get("type")))
                .toList();

        assertEquals(15L, persistedBatch.getQuantity());
        assertEquals(2, inputLogs.size());
        assertEquals(5L, inputLogs.stream()
                .filter(log -> ((Number) log.get("quantity")).longValue() == 5L)
                .findFirst()
                .orElseThrow()
                .get("quantity"));
    }

    @Test
    void shouldUseBatchIdAsDeterministicTieBreakerForSameExpiryDate() {
        LocalDate sameExpiryDate = LocalDate.of(2026, 10, 1);
        String firstBatchNumber = uniqueBatchNumber("TIE-FIRST");
        String secondBatchNumber = uniqueBatchNumber("TIE-SECOND");

        batchService.createBatch(batchRequest(firstBatchNumber, 5L, sameExpiryDate, 13L));
        batchService.createBatch(batchRequest(secondBatchNumber, 5L, sameExpiryDate, 13L));

        Batch firstBatch = batchRepository.findByBatchNumber(firstBatchNumber).orElseThrow();
        Batch secondBatch = batchRepository.findByBatchNumber(secondBatchNumber).orElseThrow();

        assertTrue(firstBatch.getId() < secondBatch.getId());

        batchService.consumeStock(new ConsumeBatchRequestDTO(13L, 1L));

        Batch persistedFirstBatch = batchRepository.findByBatchNumber(firstBatchNumber).orElseThrow();
        Batch persistedSecondBatch = batchRepository.findByBatchNumber(secondBatchNumber).orElseThrow();

        assertEquals(4L, persistedFirstBatch.getQuantity());
        assertEquals(5L, persistedSecondBatch.getQuantity());
        assertEquals(1, outputLogsForBatch(firstBatch.getId()).size());
        assertEquals(0, outputLogsForBatch(secondBatch.getId()).size());
    }

    @Test
    void shouldNotConsumeExpiredBatchFromRealDatabase() {
        String batchNumber = uniqueBatchNumber("EXPIRED");
        batchService.createBatch(batchRequest(batchNumber, 5L, LocalDate.now().minusDays(1), 13L));
        Batch expiredBatch = batchRepository.findByBatchNumber(batchNumber).orElseThrow();

        assertThrows(
                InsufficientStockException.class,
                () -> batchService.consumeStock(new ConsumeBatchRequestDTO(13L, 1000L))
        );

        Batch persistedExpiredBatch = batchRepository.findByBatchNumber(batchNumber).orElseThrow();
        assertEquals(5L, persistedExpiredBatch.getQuantity());
        assertEquals(0, outputLogsForBatch(expiredBatch.getId()).size());
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

    @Test
    void shouldRollbackAllBatchChangesAndOutputLogsAfterMultipleBatchInsufficiency() {
        String firstBatchNumber = uniqueBatchNumber("ROLLBACK-FIRST");
        String secondBatchNumber = uniqueBatchNumber("ROLLBACK-SECOND");
        batchService.createBatch(batchRequest(firstBatchNumber, 5L, LocalDate.of(2027, 5, 1), 13L));
        batchService.createBatch(batchRequest(secondBatchNumber, 5L, LocalDate.of(2027, 6, 1), 13L));

        Batch firstBatch = batchRepository.findByBatchNumber(firstBatchNumber).orElseThrow();
        Batch secondBatch = batchRepository.findByBatchNumber(secondBatchNumber).orElseThrow();

        assertThrows(
                InsufficientStockException.class,
                () -> batchService.consumeStock(new ConsumeBatchRequestDTO(13L, 1000L))
        );

        Batch persistedFirstBatch = batchRepository.findByBatchNumber(firstBatchNumber).orElseThrow();
        Batch persistedSecondBatch = batchRepository.findByBatchNumber(secondBatchNumber).orElseThrow();
        assertEquals(5L, persistedFirstBatch.getQuantity());
        assertEquals(5L, persistedSecondBatch.getQuantity());
        assertEquals(0, outputLogsForBatch(firstBatch.getId()).size());
        assertEquals(0, outputLogsForBatch(secondBatch.getId()).size());
    }

    private BatchRequestDTO batchRequest(String batchNumber, long quantity) {
        return batchRequest(batchNumber, quantity, EXPIRY_DATE);
    }

    private BatchRequestDTO batchRequest(String batchNumber, long quantity, LocalDate expiryDate) {
        return batchRequest(batchNumber, quantity, expiryDate, 1L);
    }

    private BatchRequestDTO batchRequest(String batchNumber, long quantity, LocalDate expiryDate, Long productId) {
        return new BatchRequestDTO(
                batchNumber,
                quantity,
                MANUFACTURING_DATE,
                expiryDate,
                BigDecimal.valueOf(10.00),
                productId
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
