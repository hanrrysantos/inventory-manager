package br.com.hanrry.inventory.dto.batch;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BatchResponseDTO (
        Long id,
        String batchNumber,
        Long quantity,
        LocalDate manufacturingDate,
        LocalDate expiryDate,
        BigDecimal price,
        Long productId,
        String productName
){
}
