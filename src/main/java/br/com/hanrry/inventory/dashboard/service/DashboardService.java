package br.com.hanrry.inventory.dashboard.service;

import br.com.hanrry.inventory.dashboard.dto.AttentionItemDTO;
import br.com.hanrry.inventory.dashboard.dto.DashboardSummaryDTO;
import br.com.hanrry.inventory.dashboard.dto.InventoryStatus;
import br.com.hanrry.inventory.inventory.batch.Batch;
import br.com.hanrry.inventory.product.entity.Product;
import br.com.hanrry.inventory.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryDTO getSummary() {
        List<Product> products = productRepository.findAllWithBatches();
        LocalDate today = LocalDate.now();

        long totalQuantity = 0L;
        long lowStockCount = 0L;
        long outOfStockCount = 0L;
        long expiredBatchCount = 0L;
        BigDecimal inventoryValue = BigDecimal.ZERO;
        List<AttentionItemDTO> attentionItems = new ArrayList<>();

        for (Product product : products) {
            long productQuantity = 0L;

            for (Batch batch : product.getBatches()) {
                long quantity = batch.getQuantity() == null ? 0L : batch.getQuantity();
                productQuantity += quantity;
                totalQuantity += quantity;
                inventoryValue = inventoryValue.add(batch.getPrice().multiply(BigDecimal.valueOf(quantity)));

                if (batch.getExpiryDate().isBefore(today)) {
                    expiredBatchCount++;
                }
            }

            InventoryStatus status = resolveStatus(productQuantity, product.getMinStock());
            if (status == InventoryStatus.LOW_STOCK) {
                lowStockCount++;
                attentionItems.add(toAttentionItem(product, productQuantity, status));
            } else if (status == InventoryStatus.OUT_OF_STOCK) {
                outOfStockCount++;
                attentionItems.add(toAttentionItem(product, productQuantity, status));
            }
        }

        attentionItems.sort(Comparator
                .comparingInt((AttentionItemDTO item) -> item.status() == InventoryStatus.OUT_OF_STOCK ? 0 : 1)
                .thenComparing(AttentionItemDTO::productName));

        return new DashboardSummaryDTO(
                totalQuantity,
                products.size(),
                lowStockCount,
                outOfStockCount,
                expiredBatchCount,
                inventoryValue.setScale(2, RoundingMode.HALF_UP),
                attentionItems
        );
    }

    private InventoryStatus resolveStatus(long quantity, long minStock) {
        if (quantity == 0) {
            return InventoryStatus.OUT_OF_STOCK;
        }
        if (quantity <= minStock) {
            return InventoryStatus.LOW_STOCK;
        }
        return InventoryStatus.IN_STOCK;
    }

    private AttentionItemDTO toAttentionItem(Product product, long quantity, InventoryStatus status) {
        return new AttentionItemDTO(
                product.getId(),
                product.getName(),
                product.getSku(),
                quantity,
                product.getMinStock(),
                status
        );
    }
}
