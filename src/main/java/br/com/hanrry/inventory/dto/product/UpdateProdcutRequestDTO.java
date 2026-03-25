package br.com.hanrry.inventory.dto.product;

public record UpdateProdcutRequestDTO(
        String name,
        Long minStock
) {
}
