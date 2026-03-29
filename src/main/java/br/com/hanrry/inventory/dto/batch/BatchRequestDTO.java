package br.com.hanrry.inventory.dto.batch;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BatchRequestDTO (
        String batchNumber,
        Long quantity,
        LocalDate manufacturingDate,
        LocalDate expiryDate,
        BigDecimal price,
        Long productId
){
}
