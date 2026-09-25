package br.com.hanrry.inventory.inventory.dto.batch;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ConsumeBatchRequestDTO (
        @NotNull(message = "Product is required")
        Long productId,
        @NotNull(message = "Quantity to consume is required")
        @Positive(message = "Quantity to consume must be greater than zero")
        Long quantityToConsume
){
}
