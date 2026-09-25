package br.com.hanrry.inventory.inventory.dto.invetoryLog;

import br.com.hanrry.inventory.inventory.movement.LogType;

public record InventoryLogRequestDTO(
        LogType type,
        Long quantity,
        Long batchId
) {
}
