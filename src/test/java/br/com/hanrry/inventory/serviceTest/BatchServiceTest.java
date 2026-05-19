package br.com.hanrry.inventory.serviceTest;

import br.com.hanrry.inventory.dto.batch.AddStockBatchRequestDTO;
import br.com.hanrry.inventory.dto.batch.BatchRequestDTO;
import br.com.hanrry.inventory.dto.batch.BatchResponseDTO;
import br.com.hanrry.inventory.dto.batch.ConsumeBatchRequestDTO;
import br.com.hanrry.inventory.entity.Batch;
import br.com.hanrry.inventory.entity.Product;
import br.com.hanrry.inventory.entity.enums.LogType;
import br.com.hanrry.inventory.exception.batch.BatchAlreadyExists;
import br.com.hanrry.inventory.exception.batch.BatchNotFound;
import br.com.hanrry.inventory.exception.batch.InsufficientStockException;
import br.com.hanrry.inventory.exception.batch.InvalidQuantityException;
import br.com.hanrry.inventory.exception.product.ProductNotFoundException;
import br.com.hanrry.inventory.mapper.BatchMapper;
import br.com.hanrry.inventory.repository.BatchRepository;
import br.com.hanrry.inventory.repository.ProductRepository;
import br.com.hanrry.inventory.service.BatchService;
import br.com.hanrry.inventory.service.InventoryLogService;
import br.com.hanrry.inventory.service.StockAlertService;
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

        when(batchRepository.findById(1L))
                .thenReturn(Optional.of(batch));

        when(batchRepository.save(batch))
                .thenReturn(savedBatch);

        when(batchMapper.toDTO(savedBatch))
                .thenReturn(response);

        BatchResponseDTO result = batchService.addStock(1L, request);

        assertNotNull(result);
        assertEquals(15L, result.quantity());
        assertEquals(15L, batch.getQuantity());

        verify(batchRepository).findById(1L);
        verify(batchRepository).save(batch);
        verify(inventoryLogService).createLog(savedBatch, 5L, LogType.INPUT);
        verify(batchMapper).toDTO(savedBatch);
    }

    @Test
    void shouldThrowExceptionWhenBatchNotFoundOnAddStock() {
        AddStockBatchRequestDTO request = new AddStockBatchRequestDTO(5L);

        when(batchRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                BatchNotFound.class,
                () -> batchService.addStock(99L, request)
        );

        verify(batchRepository).findById(99L);
        verify(batchRepository, never()).save(any());
        verifyNoInteractions(inventoryLogService);
    }

    @Test
    void shouldThrowExceptionWhenQuantityToAddIsInvalid() {
        AddStockBatchRequestDTO request = new AddStockBatchRequestDTO(0L);

        Batch batch = new Batch();
        batch.setId(1L);
        batch.setQuantity(10L);

        when(batchRepository.findById(1L))
                .thenReturn(Optional.of(batch));

        assertThrows(
                InvalidQuantityException.class,
                () -> batchService.addStock(1L, request)
        );

        verify(batchRepository).findById(1L);
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

        when(batchRepository.findByProductIdAndQuantityGreaterThanOrderByExpiryDateAsc(1L, 0L))
                .thenReturn(List.of(batch));

        batchService.consumeStock(request);

        assertEquals(5L, batch.getQuantity());

        verify(batchRepository)
                .findByProductIdAndQuantityGreaterThanOrderByExpiryDateAsc(1L, 0L);

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

        when(batchRepository.findByProductIdAndQuantityGreaterThanOrderByExpiryDateAsc(1L, 0L))
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
    void shouldThrowExceptionWhenStockIsInsufficient() {
        ConsumeBatchRequestDTO request = new ConsumeBatchRequestDTO(
                1L,
                20L
        );

        Batch batch = new Batch();
        batch.setId(1L);
        batch.setQuantity(5L);

        when(batchRepository.findByProductIdAndQuantityGreaterThanOrderByExpiryDateAsc(1L, 0L))
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

        List<Batch> expiredBatches = List.of(batch);
        List<BatchResponseDTO> responses = List.of(response);

        when(batchRepository.findByExpiryDateBefore(LocalDate.now()))
                .thenReturn(expiredBatches);

        when(batchMapper.toDTOList(expiredBatches))
                .thenReturn(responses);

        List<BatchResponseDTO> result = batchService.findExpiredBatches();

        assertEquals(1, result.size());
        assertEquals("BATCH-001", result.get(0).batchNumber());
        assertEquals(expiryDate, result.get(0).expiryDate());

        verify(batchRepository).findByExpiryDateBefore(LocalDate.now());
        verify(batchMapper).toDTOList(expiredBatches);
    }
}