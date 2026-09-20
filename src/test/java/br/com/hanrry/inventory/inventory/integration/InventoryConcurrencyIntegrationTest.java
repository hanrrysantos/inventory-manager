package br.com.hanrry.inventory.inventory.integration;

import br.com.hanrry.inventory.inventory.batch.Batch;
import br.com.hanrry.inventory.inventory.dto.batch.BatchRequestDTO;
import br.com.hanrry.inventory.inventory.dto.batch.AddStockBatchRequestDTO;
import br.com.hanrry.inventory.inventory.dto.batch.ConsumeBatchRequestDTO;
import br.com.hanrry.inventory.shared.exception.inventory.batch.InsufficientStockException;
import br.com.hanrry.inventory.inventory.repository.BatchRepository;
import br.com.hanrry.inventory.inventory.service.BatchService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class InventoryConcurrencyIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final LocalDate EXPIRY_DATE = LocalDate.of(2027, 12, 31);

    @Autowired
    private BatchService batchService;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private ExecutorService executor;

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void prepareDatabase() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("hanrry@email.com", null, List.of()));
        jdbcTemplate.update("UPDATE tb_products SET min_stock = 0");
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void shutdownExecutor() {
        executor.shutdownNow();
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldHoldPessimisticLockUntilTheTransactionCommits() throws Exception {
        String batchNumber = "LOCK-" + System.nanoTime();
        batchService.createBatch(new BatchRequestDTO(
                batchNumber,
                10L,
                LocalDate.of(2026, 1, 1),
                EXPIRY_DATE,
                BigDecimal.TEN,
                13L
        ));

        CountDownLatch firstTransactionLocked = new CountDownLatch(1);
        CountDownLatch releaseFirstTransaction = new CountDownLatch(1);
        CountDownLatch secondTransactionFinished = new CountDownLatch(1);

        Future<?> firstTransaction = executor.submit(() -> transactionTemplate.execute(status -> {
            List<Batch> batches = batchRepository
                    .findByProductIdAndQuantityGreaterThanAndExpiryDateGreaterThanEqualOrderByExpiryDateAscIdAsc(
                            13L, 0L, LocalDate.now());
            assertNotNull(batches.stream()
                    .filter(batch -> batchNumber.equals(batch.getBatchNumber()))
                    .findFirst()
                    .orElse(null));
            firstTransactionLocked.countDown();
            await(releaseFirstTransaction);
            return null;
        }));

        assertTrue(firstTransactionLocked.await(5, TimeUnit.SECONDS));

        Future<?> secondTransaction = executor.submit(() -> {
            try {
                transactionTemplate.execute(status -> batchRepository
                        .findByProductIdAndQuantityGreaterThanAndExpiryDateGreaterThanEqualOrderByExpiryDateAscIdAsc(
                                13L, 0L, LocalDate.now()));
            } finally {
                secondTransactionFinished.countDown();
            }
        });

        assertFalse(secondTransactionFinished.await(500, TimeUnit.MILLISECONDS));

        releaseFirstTransaction.countDown();
        firstTransaction.get(5, TimeUnit.SECONDS);
        secondTransaction.get(5, TimeUnit.SECONDS);
    }

    @Test
    void shouldAllowOnlyOneConcurrentConsumptionWhenStockIsInsufficientForBoth() throws Exception {
        String batchNumber = "CONSUME-" + System.nanoTime();
        jdbcTemplate.update("UPDATE tb_batches SET quantity = 0 WHERE product_id = 2");
        batchService.createBatch(new BatchRequestDTO(
                batchNumber,
                10L,
                LocalDate.of(2026, 1, 1),
                EXPIRY_DATE,
                BigDecimal.TEN,
                2L
        ));

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<Boolean> firstConsumption = submitConsumption(ready, start, 2L, 8L);
        Future<Boolean> secondConsumption = submitConsumption(ready, start, 2L, 8L);

        assertTrue(ready.await(5, TimeUnit.SECONDS));
        start.countDown();

        boolean firstSucceeded = firstConsumption.get(5, TimeUnit.SECONDS);
        boolean secondSucceeded = secondConsumption.get(5, TimeUnit.SECONDS);

        assertEquals(1, (firstSucceeded ? 1 : 0) + (secondSucceeded ? 1 : 0));

        Batch persistedBatch = batchRepository.findByBatchNumber(batchNumber).orElseThrow();
        assertEquals(2L, persistedBatch.getQuantity());
        assertEquals(8L, jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(quantity), 0) FROM tb_inventory_logs WHERE batch_id = ? AND type = 'OUTPUT'",
                Long.class,
                persistedBatch.getId()
        ));
        assertTrue(persistedBatch.getQuantity() >= 0);
    }

    @Test
    void shouldProtectFefoDecisionAcrossMultipleBatchesDuringConcurrentConsumption() throws Exception {
        String olderBatchNumber = "CONSUME-MULTI-OLDER-" + System.nanoTime();
        String newerBatchNumber = "CONSUME-MULTI-NEWER-" + System.nanoTime();
        jdbcTemplate.update("UPDATE tb_batches SET quantity = 0 WHERE product_id = 2");

        batchService.createBatch(new BatchRequestDTO(
                olderBatchNumber,
                5L,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 1),
                BigDecimal.TEN,
                2L
        ));
        batchService.createBatch(new BatchRequestDTO(
                newerBatchNumber,
                5L,
                LocalDate.of(2026, 1, 1),
                EXPIRY_DATE,
                BigDecimal.TEN,
                2L
        ));

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<Boolean> firstConsumption = submitConsumption(ready, start, 2L, 8L);
        Future<Boolean> secondConsumption = submitConsumption(ready, start, 2L, 8L);

        assertTrue(ready.await(5, TimeUnit.SECONDS));
        start.countDown();

        boolean firstSucceeded = firstConsumption.get(5, TimeUnit.SECONDS);
        boolean secondSucceeded = secondConsumption.get(5, TimeUnit.SECONDS);

        assertEquals(1, (firstSucceeded ? 1 : 0) + (secondSucceeded ? 1 : 0));

        Batch persistedOlderBatch = batchRepository.findByBatchNumber(olderBatchNumber).orElseThrow();
        Batch persistedNewerBatch = batchRepository.findByBatchNumber(newerBatchNumber).orElseThrow();

        assertEquals(0L, persistedOlderBatch.getQuantity());
        assertEquals(2L, persistedNewerBatch.getQuantity());
        assertEquals(8L, jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(quantity), 0) FROM tb_inventory_logs "
                        + "WHERE batch_id IN (?, ?) AND type = 'OUTPUT'",
                Long.class,
                persistedOlderBatch.getId(),
                persistedNewerBatch.getId()
        ));
        assertEquals(1L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_inventory_logs "
                        + "WHERE batch_id = ? AND type = 'OUTPUT' AND quantity = 5",
                Long.class,
                persistedOlderBatch.getId()
        ));
        assertEquals(1L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_inventory_logs "
                        + "WHERE batch_id = ? AND type = 'OUTPUT' AND quantity = 3",
                Long.class,
                persistedNewerBatch.getId()
        ));
    }

    @Test
    void shouldPersistBothConcurrentStockEntriesWithoutLostUpdate() throws Exception {
        String batchNumber = "ADD-" + System.nanoTime();
        batchService.createBatch(new BatchRequestDTO(
                batchNumber,
                10L,
                LocalDate.of(2026, 1, 1),
                EXPIRY_DATE,
                BigDecimal.TEN,
                2L
        ));
        Batch createdBatch = batchRepository.findByBatchNumber(batchNumber).orElseThrow();

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<?> firstAddition = submitAddition(ready, start, createdBatch.getId());
        Future<?> secondAddition = submitAddition(ready, start, createdBatch.getId());

        assertTrue(ready.await(5, TimeUnit.SECONDS));
        start.countDown();

        firstAddition.get(5, TimeUnit.SECONDS);
        secondAddition.get(5, TimeUnit.SECONDS);

        Batch persistedBatch = batchRepository.findByBatchNumber(batchNumber).orElseThrow();
        assertEquals(26L, persistedBatch.getQuantity());
        assertEquals(16L, jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(quantity), 0) FROM tb_inventory_logs "
                        + "WHERE batch_id = ? AND type = 'INPUT' AND quantity = 8",
                Long.class,
                persistedBatch.getId()
        ));
        assertEquals(2L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_inventory_logs "
                        + "WHERE batch_id = ? AND type = 'INPUT' AND quantity = 8",
                Long.class,
                persistedBatch.getId()
        ));
    }

    private Future<?> submitAddition(CountDownLatch ready, CountDownLatch start, Long batchId) {
        return executor.submit(() -> {
            authenticateSeedOwner();
            ready.countDown();
            await(start);
            try {
                transactionTemplate.execute(status -> {
                    batchService.addStock(batchId, new AddStockBatchRequestDTO(8L));
                    return null;
                });
                return null;
            } finally {
                SecurityContextHolder.clearContext();
            }
        });
    }

    private Future<Boolean> submitConsumption(
            CountDownLatch ready,
            CountDownLatch start,
            Long productId,
            Long quantity
    ) {
        return executor.submit(() -> {
            authenticateSeedOwner();
            ready.countDown();
            await(start);
            try {
                transactionTemplate.execute(status -> {
                    batchService.consumeStock(new ConsumeBatchRequestDTO(productId, quantity));
                    return null;
                });
                return true;
            } catch (InsufficientStockException exception) {
                return false;
            } finally {
                SecurityContextHolder.clearContext();
            }
        });
    }

    private void authenticateSeedOwner() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("hanrry@email.com", null, List.of()));
    }

    private void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for transaction release", exception);
        }
    }
}
