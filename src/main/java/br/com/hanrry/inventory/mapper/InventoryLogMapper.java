package br.com.hanrry.inventory.mapper;

import br.com.hanrry.inventory.dto.invetoryLog.InventoryLogResponseDTO;
import br.com.hanrry.inventory.entity.InventoryLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface InventoryLogMapper {

    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "batchId", source = "batch.id")
    InventoryLogResponseDTO toDTO(InventoryLog log);

    List<InventoryLogResponseDTO> toDTOList(List<InventoryLog> logs);
}