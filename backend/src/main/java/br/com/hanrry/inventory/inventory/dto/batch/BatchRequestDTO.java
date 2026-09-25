package br.com.hanrry.inventory.inventory.dto.batch;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record BatchRequestDTO (
        @NotBlank(message = "Batch number is required")
        @Size(max = 100, message = "Batch number must have at most 100 characters")
        String batchNumber,
        @NotNull(message = "Quantity is required")
        @PositiveOrZero(message = "Quantity must be greater than or equal to zero")
        Long quantity,
        @NotNull(message = "Manufacturing date is required")
        LocalDate manufacturingDate,
        @NotNull(message = "Expiry date is required")
        LocalDate expiryDate,
        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", message = "Price must be greater than or equal to zero")
        BigDecimal price,
        @NotNull(message = "Product is required")
        Long productId
){
}
