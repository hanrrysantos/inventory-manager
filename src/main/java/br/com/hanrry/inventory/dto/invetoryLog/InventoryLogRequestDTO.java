package br.com.hanrry.inventory.dto.invetoryLog;

import br.com.hanrry.inventory.entity.enums.LogType;

public record InventoryLogRequestDTO(
        LogType type,
        Long quantity,
        Long batchId
) {
}
