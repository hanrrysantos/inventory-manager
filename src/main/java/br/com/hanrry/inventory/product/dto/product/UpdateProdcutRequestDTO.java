package br.com.hanrry.inventory.product.dto.product;

public record UpdateProdcutRequestDTO(
        String name,
        Long minStock
) {
}
