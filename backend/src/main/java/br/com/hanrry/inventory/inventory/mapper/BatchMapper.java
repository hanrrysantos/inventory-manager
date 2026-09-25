package br.com.hanrry.inventory.inventory.mapper;

import br.com.hanrry.inventory.inventory.dto.batch.BatchRequestDTO;
import br.com.hanrry.inventory.inventory.dto.batch.BatchResponseDTO;
import br.com.hanrry.inventory.inventory.batch.Batch;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BatchMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    Batch toEntity(BatchRequestDTO request);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    BatchResponseDTO toDTO(Batch batch);

    List<BatchResponseDTO> toDTOList(List<Batch> batch);
}
