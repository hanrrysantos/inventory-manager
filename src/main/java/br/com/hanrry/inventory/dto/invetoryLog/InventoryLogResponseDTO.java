package br.com.hanrry.inventory.dto.invetoryLog;

import br.com.hanrry.inventory.entity.LogType;

import java.time.LocalDateTime;

public record InventoryLogResponseDTO(
        Long id,
        LogType type,
        LocalDateTime timestamp,
        Long quantity,
        Long batchId,
        String productName
) {
}
