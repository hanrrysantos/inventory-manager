package br.com.hanrry.inventory.product.dto.product;

public record ProductRequestDTO (

        String name,
        String sku,
        Long minStock,
        Long categoryId
){
}
