package br.com.hanrry.inventory.service;

import br.com.hanrry.inventory.dto.batch.AddStockBatchRequestDTO;
import br.com.hanrry.inventory.dto.batch.BatchRequestDTO;
import br.com.hanrry.inventory.dto.batch.BatchResponseDTO;
import br.com.hanrry.inventory.dto.batch.ConsumeBatchRequestDTO;
import br.com.hanrry.inventory.entity.Batch;
import br.com.hanrry.inventory.entity.enums.LogType;
import br.com.hanrry.inventory.entity.Product;
import br.com.hanrry.inventory.exception.batch.BatchAlreadyExists;
import br.com.hanrry.inventory.exception.batch.BatchNotFound;
import br.com.hanrry.inventory.exception.batch.InvalidQuantityException;
import br.com.hanrry.inventory.exception.batch.InsufficientStockException;
import br.com.hanrry.inventory.exception.product.ProductNotFoundException;
import br.com.hanrry.inventory.mapper.BatchMapper;
import br.com.hanrry.inventory.repository.BatchRepository;
import br.com.hanrry.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class    BatchService {

    private final BatchMapper batchMapper;
    private final BatchRepository batchRepository;
    private final InventoryLogService inventoryLogService;
    private final ProductRepository productRepository;
    private final StockAlertService stockAlertService;

    @Transactional
    public BatchResponseDTO createBatch(BatchRequestDTO request) {
        batchRepository.findByBatchNumber(request.batchNumber()).ifPresent(
                b -> {
                    throw new BatchAlreadyExists("Batch already exists");
                });

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ProductNotFoundException
                        ("Product not found with this id: " + request.productId())
                );

        Batch batch = batchMapper.toEntity(request);
        batch.setProduct(product);

        Batch savedBatch = batchRepository.save(batch);

        inventoryLogService.createLog(savedBatch, savedBatch.getQuantity(), LogType.INPUT);

        return batchMapper.toDTO(savedBatch);
    }

    @Transactional
    public BatchResponseDTO addStock(Long id, AddStockBatchRequestDTO request) {
        Batch batch = batchRepository.findById(id)
                .orElseThrow(() -> new BatchNotFound
                        ("Batch not found with this id: " + id));

        Long quantityToAdd = request.quantityToAdd();

        if (quantityToAdd <= 0) {
            throw new InvalidQuantityException
                    ("The quantity must be greater than 0");
        }

        batch.setQuantity(batch.getQuantity() + quantityToAdd);
        Batch savedBatch = batchRepository.save(batch);

        inventoryLogService.createLog(savedBatch, quantityToAdd, LogType.INPUT);

        return batchMapper.toDTO(savedBatch);
    }

    @Transactional
    public void consumeStock(ConsumeBatchRequestDTO request) {

        List<Batch> batches = batchRepository.
                findByProductIdAndQuantityGreaterThanOrderByExpiryDateAsc(request.productId(), 0L);

        long howMuchNeedGet = request.quantityToConsume();

        for (Batch batch : batches) {
            if (howMuchNeedGet <= 0) break;

            long stockAvaliableInTheCurrentPack = batch.getQuantity();
            long unitsRemovedFromTheBatch;

            if (stockAvaliableInTheCurrentPack <= howMuchNeedGet) {
                unitsRemovedFromTheBatch = stockAvaliableInTheCurrentPack;
                howMuchNeedGet -= unitsRemovedFromTheBatch;
                batch.setQuantity(0L);
            } else {
                unitsRemovedFromTheBatch = howMuchNeedGet;
                batch.setQuantity(stockAvaliableInTheCurrentPack - howMuchNeedGet);
                howMuchNeedGet = 0;
            }

            inventoryLogService.createLog(batch, unitsRemovedFromTheBatch, LogType.OUTPUT);
        }

        if (howMuchNeedGet > 0) {
            throw new InsufficientStockException("Insufficient Stock");
        }

        this.stockAlertService.checkInventoryAndNotify();
    }

    public List<BatchResponseDTO> findExpiredBatches() {
        List<Batch> batchesExpired = batchRepository.findByExpiryDateBefore(LocalDate.now());

        return batchMapper.toDTOList(batchesExpired);
    }
}