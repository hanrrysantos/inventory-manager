package br.com.hanrry.inventory.dto.invetoryLog;

import br.com.hanrry.inventory.entity.Type;

public record InventoryLogRequestDTO(
        Type type,
        Long quantity,
        Long batchId
) {
}
