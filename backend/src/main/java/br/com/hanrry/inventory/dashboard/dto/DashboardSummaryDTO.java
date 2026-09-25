package br.com.hanrry.inventory.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummaryDTO(
        Long totalQuantity,
        long productCount,
        long lowStockCount,
        long outOfStockCount,
        long expiredBatchCount,
        BigDecimal inventoryValue,
        List<AttentionItemDTO> attentionItems
) {
}
