package br.com.hanrry.inventory.dto.batch;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BatchRequestDTO (
        Long batchNumber,
        Long quantity,
        LocalDate expiryDate,
        BigDecimal price,
        Long productId
){
}
