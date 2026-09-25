package br.com.hanrry.inventory.inventory.serviceTest;

import br.com.hanrry.inventory.inventory.dto.batch.AddStockBatchRequestDTO;
import br.com.hanrry.inventory.inventory.dto.batch.BatchRequestDTO;
import br.com.hanrry.inventory.inventory.dto.batch.BatchResponseDTO;
import br.com.hanrry.inventory.inventory.dto.batch.ConsumeBatchRequestDTO;
import br.com.hanrry.inventory.inventory.batch.Batch;
import br.com.hanrry.inventory.product.entity.Product;
import br.com.hanrry.inventory.inventory.movement.LogType;
import br.com.hanrry.inventory.shared.exception.inventory.batch.BatchAlreadyExists;
import br.com.hanrry.inventory.shared.exception.inventory.batch.BatchNotFound;
import br.com.hanrry.inventory.shared.exception.inventory.batch.InsufficientStockException;
import br.com.hanrry.inventory.shared.exception.inventory.batch.InvalidQuantityException;
import br.com.hanrry.inventory.shared.exception.product.product.ProductNotFoundException;
import br.com.hanrry.inventory.inventory.mapper.BatchMapper;
import br.com.hanrry.inventory.inventory.repository.BatchRepository;
import br.com.hanrry.inventory.product.repository.ProductRepository;
import br.com.hanrry.inventory.inventory.service.BatchService;
import br.com.hanrry.inventory.inventory.service.InventoryLogService;
import br.com.hanrry.inventory.notification.service.StockAlertService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchServiceTest {

    @Mock
    private BatchMapper batchMapper;

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private InventoryLogService inventoryLogService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockAlertService stockAlertService;

    @InjectMocks
    private BatchService batchService;

    @Test
    void shouldCreateBatchSuccessfully() {
        LocalDate manufacturingDate = LocalDate.now().minusDays(10);
        LocalDate expiryDate = LocalDate.now().plusMonths(6);

        BatchRequestDTO request = new BatchRequestDTO(
                "BATCH-001",
                10L,
                manufacturingDate,
                expiryDate,
                BigDecimal.valueOf(2500.00),
                1L
        );

        Product product = new Product();
        product.setId(1L);
        product.setName("Notebook");

        Batch batch = new Batch();
        batch.setBatchNumber("BATCH-001");
        batch.setQuantity(10L);
        batch.setManufacturingDate(manufacturingDate);
        batch.setExpiryDate(expiryDate);
        batch.setPrice(BigDecimal.valueOf(2500.00));

        Batch savedBatch = new Batch();
        savedBatch.setId(1L);
        savedBatch.setBatchNumber("BATCH-001");
        savedBatch.setQuantity(10L);
        savedBatch.setManufacturingDate(manufacturingDate);
        savedBatch.setExpiryDate(expiryDate);
        savedBatch.setPrice(BigDecimal.valueOf(2500.00));
        savedBatch.setProduct(product);

        BatchResponseDTO response = new BatchResponseDTO(
                1L,
                "BATCH-001",
                10L,
                manufacturingDate,
                expiryDate,
                BigDecimal.valueOf(2500.00),
                1L,
                "Notebook"
        );

        when(batchRepository.findByBatchNumber(request.batchNumber()))
                .thenReturn(Optional.empty());

        when(productRepository.findById(request.productId()))
                .thenReturn(Optional.of(product));

        when(batchMapper.toEntity(request))
                .thenReturn(batch);

        when(batchRepository.save(batch))
                .thenReturn(savedBatch);

        when(batchMapper.toDTO(savedBatch))
                .thenReturn(response);

        BatchResponseDTO result = batchService.createBatch(request);

        assertNotNull(result);
        assertEquals("BATCH-001", result.batchNumber());
        assertEquals(10L, result.quantity());
        assertEquals("Notebook", result.productName());

        verify(batchRepository).findByBatchNumber("BATCH-001");
        verify(productRepository).findById(1L);
        verify(batchMapper).toEntity(request);
        verify(batchRepository).save(batch);
        verify(inventoryLogService).createLog(savedBatch, 10L, LogType.INPUT);
        verify(batchMapper).toDTO(savedBatch);
    }

    @Test
    void shouldThrowExceptionWhenBatchAlreadyExists() {
        BatchRequestDTO request = new BatchRequestDTO(
                "BATCH-001",
                10L,
                LocalDate.now().minusDays(10),
                LocalDate.now().plusMonths(6),
                BigDecimal.valueOf(2500.00),
                1L
        );

        Batch existingBatch = new Batch();

        when(batchRepository.findByBatchNumber(request.batchNumber()))
                .thenReturn(Optional.of(existingBatch));

        assertThrows(
                BatchAlreadyExists.class,
                () -> batchService.createBatch(request)
        );

        verify(batchRepository).findByBatchNumber("BATCH-001");
        verifyNoInteractions(productRepository);
        verifyNoInteractions(batchMapper);
        verify(batchRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenProductNotFoundOnCreateBatch() {
        BatchRequestDTO request = new BatchRequestDTO(
                "BATCH-001",
                10L,
                LocalDate.now().minusDays(10),
                LocalDate.now().plusMonths(6),
                BigDecimal.valueOf(2500.00),
                99L
        );

        when(batchRepository.findByBatchNumber(request.batchNumber()))
                .thenReturn(Optional.empty());

        when(productRepository.findById(request.productId()))
                .thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> batchService.createBatch(request)
        );

        verify(batchRepository).findByBatchNumber("BATCH-001");
        verify(productRepository).findById(99L);
        verify(batchRepository, never()).save(any());
    }

    @Test
    void shouldAddStockSuccessfully() {
        AddStockBatchRequestDTO request = new AddStockBatchRequestDTO(5L);

        Batch batch = new Batch();
        batch.setId(1L);
        batch.setQuantity(10L);

        Batch savedBatch = new Batch();
        savedBatch.setId(1L);
        savedBatch.setQuantity(15L);

        BatchResponseDTO response = new BatchResponseDTO(
                1L,
                "BATCH-001",
                15L,
                LocalDate.now().minusDays(10),
                LocalDate.now().plusMonths(6),
                BigDecimal.valueOf(2500.00),
                1L,
                "Notebook"
        );

        when(batchRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(batch));

        when(batchRepository.save(batch))
                .thenReturn(savedBatch);

        when(batchMapper.toDTO(savedBatch))
                .thenReturn(response);

        BatchResponseDTO result = batchService.addStock(1L, request);

        assertNotNull(result);
        assertEquals(15L, result.quantity());
        assertEquals(15L, batch.getQuantity());

        verify(batchRepository).findByIdForUpdate(1L);
        verify(batchRepository).save(batch);
        verify(inventoryLogService).createLog(savedBatch, 5L, LogType.INPUT);
        verify(batchMapper).toDTO(savedBatch);
    }

    @Test
    void shouldThrowExceptionWhenBatchNotFoundOnAddStock() {
        AddStockBatchRequestDTO request = new AddStockBatchRequestDTO(5L);

        when(batchRepository.findByIdForUpdate(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                BatchNotFound.class,
                () -> batchService.addStock(99L, request)
        );

        verify(batchRepository).findByIdForUpdate(99L);
        verify(batchRepository, never()).save(any());
        verifyNoInteractions(inventoryLogService);
    }

    @Test
    void shouldThrowExceptionWhenQuantityToAddIsInvalid() {
        AddStockBatchRequestDTO request = new AddStockBatchRequestDTO(0L);

        Batch batch = new Batch();
        batch.setId(1L);
        batch.setQuantity(10L);

        when(batchRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(batch));

        assertThrows(
                InvalidQuantityException.class,
                () -> batchService.addStock(1L, request)
        );

        verify(batchRepository).findByIdForUpdate(1L);
        verify(batchRepository, never()).save(any());
        verifyNoInteractions(inventoryLogService);
    }

    @Test
    void shouldConsumeStockFromOldestBatchSuccessfully() {
        ConsumeBatchRequestDTO request = new ConsumeBatchRequestDTO(
                1L,
                5L
        );

        Batch batch = new Batch();
        batch.setId(1L);
        batch.setQuantity(10L);
        batch.setExpiryDate(LocalDate.now().plusDays(10));

        when(batchRepository.findByProductIdAndQuantityGreaterThanAndExpiryDateGreaterThanEqualOrderByExpiryDateAscIdAsc(
                1L, 0L, LocalDate.now()))
                .thenReturn(List.of(batch));

        batchService.consumeStock(request);

        assertEquals(5L, batch.getQuantity());

        verify(batchRepository)
                .findByProductIdAndQuantityGreaterThanAndExpiryDateGreaterThanEqualOrderByExpiryDateAscIdAsc(
                        1L, 0L, LocalDate.now());

        verify(inventoryLogService)
                .createLog(batch, 5L, LogType.OUTPUT);

        verify(stockAlertService)
                .checkInventoryAndNotify();
    }

    @Test
    void shouldConsumeStockFromMultipleBatchesSuccessfully() {
        ConsumeBatchRequestDTO request = new ConsumeBatchRequestDTO(
                1L,
                12L
        );

        Batch firstBatch = new Batch();
        firstBatch.setId(1L);
        firstBatch.setQuantity(5L);
        firstBatch.setExpiryDate(LocalDate.now().plusDays(5));

        Batch secondBatch = new Batch();
        secondBatch.setId(2L);
        secondBatch.setQuantity(10L);
        secondBatch.setExpiryDate(LocalDate.now().plusDays(20));

        when(batchRepository.findByProductIdAndQuantityGreaterThanAndExpiryDateGreaterThanEqualOrderByExpiryDateAscIdAsc(
                1L, 0L, LocalDate.now()))
                .thenReturn(List.of(firstBatch, secondBatch));

        batchService.consumeStock(request);

        assertEquals(0L, firstBatch.getQuantity());
        assertEquals(3L, secondBatch.getQuantity());

        verify(inventoryLogService)
                .createLog(firstBatch, 5L, LogType.OUTPUT);

        verify(inventoryLogService)
                .createLog(secondBatch, 7L, LogType.OUTPUT);

        verify(stockAlertService)
                .checkInventoryAndNotify();
    }

    @Test
    void shouldNotConsumeStockFromExpiredBatch() {
        ConsumeBatchRequestDTO request = new ConsumeBatchRequestDTO(
                1L,
                1L
        );

        when(batchRepository.findByProductIdAndQuantityGreaterThanAndExpiryDateGreaterThanEqualOrderByExpiryDateAscIdAsc(
                1L, 0L, LocalDate.now()))
                .thenReturn(List.of());

        assertThrows(
                InsufficientStockException.class,
                () -> batchService.consumeStock(request)
        );

        verifyNoInteractions(inventoryLogService);
        verifyNoInteractions(stockAlertService);
    }

    @Test
    void shouldConsumeFromOneOfBatchesWithSameExpiryDateWithoutCharacterizingTieOrder() {
        ConsumeBatchRequestDTO request = new ConsumeBatchRequestDTO(
                1L,
                1L
        );

        Batch firstBatch = new Batch();
        firstBatch.setId(1L);
        firstBatch.setQuantity(5L);
        firstBatch.setExpiryDate(LocalDate.of(2027, 1, 1));

        Batch secondBatch = new Batch();
        secondBatch.setId(2L);
        secondBatch.setQuantity(5L);
        secondBatch.setExpiryDate(LocalDate.of(2027, 1, 1));

        when(batchRepository.findByProductIdAndQuantityGreaterThanAndExpiryDateGreaterThanEqualOrderByExpiryDateAscIdAsc(
                1L, 0L, LocalDate.now()))
                .thenReturn(List.of(firstBatch, secondBatch));

        batchService.consumeStock(request);

        assertEquals(9L, firstBatch.getQuantity() + secondBatch.getQuantity());
        verify(inventoryLogService, times(1))
                .createLog(any(Batch.class), eq(1L), eq(LogType.OUTPUT));
        verify(stockAlertService)
                .checkInventoryAndNotify();
    }

    @Test
    void shouldKeepCurrentNoOpBehaviorWhenConsumptionQuantityIsZero() {
        ConsumeBatchRequestDTO request = new ConsumeBatchRequestDTO(
                1L,
                0L
        );

        Batch batch = new Batch();
        batch.setId(1L);
        batch.setQuantity(5L);

        when(batchRepository.findByProductIdAndQuantityGreaterThanAndExpiryDateGreaterThanEqualOrderByExpiryDateAscIdAsc(
                1L, 0L, LocalDate.now()))
                .thenReturn(List.of(batch));

        batchService.consumeStock(request);

        assertEquals(5L, batch.getQuantity());
        verifyNoInteractions(inventoryLogService);
        verify(stockAlertService)
                .checkInventoryAndNotify();
    }

    @Test
    void shouldKeepCurrentNoOpBehaviorWhenConsumptionQuantityIsNegative() {
        ConsumeBatchRequestDTO request = new ConsumeBatchRequestDTO(
                1L,
                -1L
        );

        Batch batch = new Batch();
        batch.setId(1L);
        batch.setQuantity(5L);

        when(batchRepository.findByProductIdAndQuantityGreaterThanAndExpiryDateGreaterThanEqualOrderByExpiryDateAscIdAsc(
                1L, 0L, LocalDate.now()))
                .thenReturn(List.of(batch));

        batchService.consumeStock(request);

        assertEquals(5L, batch.getQuantity());
        verifyNoInteractions(inventoryLogService);
        verify(stockAlertService)
                .checkInventoryAndNotify();
    }

    @Test
    void shouldThrowExceptionWhenStockIsInsufficient() {
        ConsumeBatchRequestDTO request = new ConsumeBatchRequestDTO(
                1L,
                20L
        );

        Batch batch = new Batch();
        batch.setId(1L);
        batch.setQuantity(5L);

        when(batchRepository.findByProductIdAndQuantityGreaterThanAndExpiryDateGreaterThanEqualOrderByExpiryDateAscIdAsc(
                1L, 0L, LocalDate.now()))
                .thenReturn(List.of(batch));

        assertThrows(
                InsufficientStockException.class,
                () -> batchService.consumeStock(request)
        );

        assertEquals(0L, batch.getQuantity());

        verify(inventoryLogService)
                .createLog(batch, 5L, LogType.OUTPUT);

        verify(stockAlertService, never())
                .checkInventoryAndNotify();
    }

    @Test
    void shouldFindExpiredBatchesSuccessfully() {
        LocalDate manufacturingDate = LocalDate.now().minusMonths(8);
        LocalDate expiryDate = LocalDate.now().minusDays(1);

        Batch batch = new Batch();
        batch.setId(1L);
        batch.setBatchNumber("BATCH-001");
        batch.setQuantity(10L);
        batch.setManufacturingDate(manufacturingDate);
        batch.setExpiryDate(expiryDate);
        batch.setPrice(BigDecimal.valueOf(2500.00));

        BatchResponseDTO response = new BatchResponseDTO(
                1L,
                "BATCH-001",
                10L,
                manufacturingDate,
                expiryDate,
                BigDecimal.valueOf(2500.00),
                1L,
                "Notebook"
        );

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 20);
        org.springframework.data.domain.Page<Batch> batchPage =
                new org.springframework.data.domain.PageImpl<>(List.of(batch), pageable, 1);

        when(batchRepository.findExpiredBatches(eq(LocalDate.now()), isNull(), eq(pageable)))
                .thenReturn(batchPage);

        when(batchMapper.toDTO(batch)).thenReturn(response);

        var result = batchService.findExpiredBatches(pageable);

        assertEquals(1, result.content().size());
        assertEquals("BATCH-001", result.content().getFirst().batchNumber());
        assertEquals(expiryDate, result.content().getFirst().expiryDate());

        verify(batchRepository).findExpiredBatches(eq(LocalDate.now()), isNull(), eq(pageable));
        verify(batchMapper).toDTO(batch);
    }

    @Test
    void shouldFindBatchesByProductIdSuccessfully() {
        Product product = new Product();
        product.setId(5L);

        Batch batch = new Batch();
        batch.setId(1L);
        batch.setBatchNumber("LOT-001");

        BatchResponseDTO response = new BatchResponseDTO(
                1L,
                "LOT-001",
                10L,
                LocalDate.now().minusMonths(1),
                LocalDate.now().plusMonths(6),
                BigDecimal.TEN,
                5L,
                "Notebook"
        );

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 20);
        org.springframework.data.domain.Page<Batch> batchPage =
                new org.springframework.data.domain.PageImpl<>(List.of(batch), pageable, 1);

        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(batchRepository.findByProductId(5L, null, pageable)).thenReturn(batchPage);
        when(batchMapper.toDTO(batch)).thenReturn(response);

        var result = batchService.findBatchesByProductId(5L, pageable);

        assertEquals(1, result.content().size());
        verify(batchRepository).findByProductId(5L, null, pageable);
    }
}
