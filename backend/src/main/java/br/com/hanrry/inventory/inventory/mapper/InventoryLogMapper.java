package br.com.hanrry.inventory.inventory.mapper;

import br.com.hanrry.inventory.inventory.dto.invetoryLog.InventoryLogResponseDTO;
import br.com.hanrry.inventory.inventory.movement.InventoryLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface InventoryLogMapper {

    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "batchId", source = "batch.id")
    @Mapping(target = "batchNumber", source = "batch.batchNumber")
    InventoryLogResponseDTO toDTO(InventoryLog log);

    List<InventoryLogResponseDTO> toDTOList(List<InventoryLog> logs);
}
