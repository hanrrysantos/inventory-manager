package br.com.hanrry.inventory.dto.product;

public record ProductResponseDTO (
        Long id,
        String name,
        String sku,
        Long totalQuantity,
        String categoryName

){
}
