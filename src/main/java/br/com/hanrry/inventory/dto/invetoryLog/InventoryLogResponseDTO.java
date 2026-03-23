package br.com.hanrry.inventory.dto.invetoryLog;

import br.com.hanrry.inventory.entity.Type;

import java.time.LocalDateTime;

public record InventoryLogResponseDTO(
        Long id,
        Type type,
        LocalDateTime timestamp,
        Long quantity,
        Long batchId,
        String productName
) {
}
