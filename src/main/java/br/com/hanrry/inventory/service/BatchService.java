package br.com.hanrry.inventory.service;

import br.com.hanrry.inventory.dto.batch.BatchRequestDTO;
import br.com.hanrry.inventory.dto.batch.BatchResponseDTO;
import br.com.hanrry.inventory.dto.product.ProductResponseDTO;
import br.com.hanrry.inventory.entity.Batch;
import br.com.hanrry.inventory.entity.LogType;
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
    private final ProductService productService;
    private final PdfService pdfService;
    private final EmailService emailService;

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
    public BatchResponseDTO addStockToBatch(Long id, Long quantity) {
        Batch batch = batchRepository.findById(id)
                .orElseThrow(() -> new BatchNotFound
                        ("Batch not found with this id: " + id));

        if (quantity <= 0) {
            throw new InvalidQuantityException
                    ("The quantity must be greater than 0");
        }

        batch.setQuantity(batch.getQuantity() + quantity);
        Batch savedBatch = batchRepository.save(batch);

        inventoryLogService.createLog(savedBatch, quantity, LogType.INPUT);

        return batchMapper.toDTO(savedBatch);
    }

    @Transactional
    public void consumeStock(Long productId, Long totalQueOClienteQuer) {
        List<Batch> batches = batchRepository.
                findByProductIdAndQuantityGreaterThanOrderByExpiryDateAsc(productId, 0L);

        long oQuantoAindaFaltaPegar = totalQueOClienteQuer;

        for (Batch batch : batches) {
            if (oQuantoAindaFaltaPegar <= 0) break;

            long estoqueDisponivelNoLoteAtual = batch.getQuantity();
            long unidadesRetiradasDesteLote;

            if (estoqueDisponivelNoLoteAtual <= oQuantoAindaFaltaPegar) {
                unidadesRetiradasDesteLote = estoqueDisponivelNoLoteAtual;
                oQuantoAindaFaltaPegar -= unidadesRetiradasDesteLote;
                batch.setQuantity(0L);
            } else {
                unidadesRetiradasDesteLote = oQuantoAindaFaltaPegar;
                batch.setQuantity(estoqueDisponivelNoLoteAtual - oQuantoAindaFaltaPegar);
                oQuantoAindaFaltaPegar = 0;
            }

            inventoryLogService.createLog(batch, unidadesRetiradasDesteLote, LogType.OUTPUT);
        }

        if (oQuantoAindaFaltaPegar > 0) {
            throw new InsufficientStockException("Insufficient Stock");
        }

        this.checkLowStockAndNotify(productId);
    }

    public List<BatchResponseDTO> findExpiredBatches() {
        return batchMapper.toDTOList(batchRepository.findByExpiryDateBefore(LocalDate.now()));
    }

    private void checkLowStockAndNotify(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException
                        ("Product not found with this id: " + productId)
                );

        long currentStock = 0;
        List<Batch> activeBatches = batchRepository.findByProductId(productId);
        for (Batch b : activeBatches) {
            currentStock += b.getQuantity();
        }

        if (currentStock <= product.getMinStock()) {
            this.sendNotificationWithReport(product.getName());
        }
    }

    public byte[] generateLowStockReport() {
        List<ProductResponseDTO> lowStockProducts = productService.getLowStockProducts();
        if (lowStockProducts.isEmpty()) return new byte[0];

        // Chama o serviço real que você criou
        return pdfService.generateLowStockReport(lowStockProducts);
    }

    public void sendNotificationWithReport(String productName) {
        byte[] pdfReport = generateLowStockReport();
        if (pdfReport.length > 0) {
            // Chama o serviço de e-mail real com o anexo
            emailService.sendLowStockAlert(productName, pdfReport);
        }
    }
}