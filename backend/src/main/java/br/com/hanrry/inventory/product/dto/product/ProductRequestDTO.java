package br.com.hanrry.inventory.product.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProductRequestDTO (

        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must have at most 255 characters")
        String name,
        @NotBlank(message = "SKU is required")
        @Size(max = 50, message = "SKU must have at most 50 characters")
        String sku,
        @NotNull(message = "Minimum stock is required")
        @PositiveOrZero(message = "Minimum stock must be greater than or equal to zero")
        Long minStock,
        @NotNull(message = "Category is required")
        Long categoryId
){
}
