package br.com.hanrry.inventory.inventory.dto.batch;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddStockBatchRequestDTO(
        @NotNull(message = "Quantity to add is required")
        @Positive(message = "Quantity to add must be greater than zero")
        Long quantityToAdd
) {
}
