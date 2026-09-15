package br.com.hanrry.inventory.inventory.dto.batch;

public record ConsumeBatchRequestDTO (
        Long productId,
        Long quantityToConsume
){
}
