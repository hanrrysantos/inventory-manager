package br.com.hanrry.inventory.dashboard.dto;

public record AttentionItemDTO(
        Long productId,
        String productName,
        String sku,
        Long quantity,
        Long minStock,
        InventoryStatus status
) {
}
