package br.com.hanrry.inventory.inventory.service;

import br.com.hanrry.inventory.inventory.dto.batch.AddStockBatchRequestDTO;
import br.com.hanrry.inventory.inventory.dto.batch.BatchRequestDTO;
import br.com.hanrry.inventory.inventory.dto.batch.BatchResponseDTO;
import br.com.hanrry.inventory.inventory.dto.batch.ConsumeBatchRequestDTO;
import br.com.hanrry.inventory.inventory.batch.Batch;
import br.com.hanrry.inventory.inventory.movement.LogType;
import br.com.hanrry.inventory.product.entity.Product;
import br.com.hanrry.inventory.shared.exception.inventory.batch.BatchAlreadyExists;
import br.com.hanrry.inventory.shared.exception.inventory.batch.BatchNotFound;
import br.com.hanrry.inventory.shared.exception.inventory.batch.InvalidQuantityException;
import br.com.hanrry.inventory.shared.exception.inventory.batch.InsufficientStockException;
import br.com.hanrry.inventory.shared.exception.product.product.ProductNotFoundException;
import br.com.hanrry.inventory.inventory.mapper.BatchMapper;
import br.com.hanrry.inventory.inventory.repository.BatchRepository;
import br.com.hanrry.inventory.product.repository.ProductRepository;
import br.com.hanrry.inventory.notification.service.StockAlertService;
import lombok.RequiredArgsConstructor;
import br.com.hanrry.inventory.shared.dto.PageResponse;
import br.com.hanrry.inventory.shared.security.OwnerContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BatchService {

    private final BatchMapper batchMapper;
    private final BatchRepository batchRepository;
    private final InventoryLogService inventoryLogService;
    private final ProductRepository productRepository;
    private final StockAlertService stockAlertService;
    private final OwnerContext ownerContext;

    @Transactional
    public BatchResponseDTO createBatch(BatchRequestDTO request) {
        batchRepository.findByBatchNumber(request.batchNumber()).ifPresent(
                b -> {
                    throw new BatchAlreadyExists("Batch already exists");
                });

        var owner = ownerContext == null ? null : ownerContext.currentUser();
        Product product = (owner == null ? productRepository.findById(request.productId()) : productRepository.findByIdAndOwner(request.productId(), owner))
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
        Batch batch = batchRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BatchNotFound
                        ("Batch not found with this id: " + id));
        var owner = ownerContext == null ? null : ownerContext.currentUser();
        if (owner != null && (batch.getProduct().getOwner() != null && !owner.equals(batch.getProduct().getOwner()))) {
            throw new BatchNotFound("Batch not found with this id: " + id);
        }

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

        var owner = ownerContext == null ? null : ownerContext.currentUser();
        if (owner != null) {
            productRepository.findByIdAndOwner(request.productId(), owner)
                    .orElseThrow(() -> new ProductNotFoundException("Product not found with this id: " + request.productId()));
        }

        List<Batch> batches = batchRepository.
                findByProductIdAndQuantityGreaterThanAndExpiryDateGreaterThanEqualOrderByExpiryDateAscIdAsc(
                        request.productId(), 0L, LocalDate.now());

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

    public PageResponse<BatchResponseDTO> findExpiredBatches(Pageable pageable) {
        var owner = ownerContext == null ? null : ownerContext.currentUser();
        Page<Batch> page = batchRepository.findExpiredBatches(LocalDate.now(), owner, pageable);
        return PageResponse.from(page, batchMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public PageResponse<BatchResponseDTO> findBatchesByProductId(Long productId, Pageable pageable) {
        var owner = ownerContext == null ? null : ownerContext.currentUser();
        (owner == null ? productRepository.findById(productId) : productRepository.findByIdAndOwner(productId, owner))
                .orElseThrow(() -> new ProductNotFoundException("Product not found with this id: " + productId));
        Page<Batch> page = batchRepository.findByProductId(productId, owner, pageable);
        return PageResponse.from(page, batchMapper::toDTO);
    }
}
