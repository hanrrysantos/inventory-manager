package br.com.hanrry.inventory.inventory.dto.invetoryLog;

import br.com.hanrry.inventory.inventory.movement.LogType;

import java.time.LocalDateTime;

public record InventoryLogResponseDTO(
        Long id,
        LogType type,
        LocalDateTime timestamp,
        Long quantity,
        Long productId,
        Long batchId,
        String productName,
        String batchNumber
) {
}
