package br.com.hanrry.inventory.product.dto.product;

public record ProductResponseDTO (
        Long id,
        String name,
        String sku,
        Long totalQuantity,
        String categoryName,
        Long minStock
){
}
