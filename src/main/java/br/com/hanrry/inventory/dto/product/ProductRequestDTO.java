package br.com.hanrry.inventory.dto.product;

public record ProductRequestDTO (

        String name,
        String sku,
        Long minStock,
        Long categoryId
){
}
