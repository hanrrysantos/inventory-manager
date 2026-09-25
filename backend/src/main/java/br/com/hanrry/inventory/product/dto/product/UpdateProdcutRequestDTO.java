package br.com.hanrry.inventory.product.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateProdcutRequestDTO(
        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must have at most 255 characters")
        String name,
        @NotNull(message = "Minimum stock is required")
        @PositiveOrZero(message = "Minimum stock must be greater than or equal to zero")
        Long minStock
) {
}
