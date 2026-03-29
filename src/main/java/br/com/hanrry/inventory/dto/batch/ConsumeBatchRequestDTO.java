package br.com.hanrry.inventory.dto.batch;

public record ConsumeBatchRequestDTO (
        Long productId,
        Long quantityToConsume
){
}
