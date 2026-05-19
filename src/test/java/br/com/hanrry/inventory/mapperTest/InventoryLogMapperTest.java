package br.com.hanrry.inventory.mapperTest;

import br.com.hanrry.inventory.dto.invetoryLog.InventoryLogResponseDTO;
import br.com.hanrry.inventory.entity.Batch;
import br.com.hanrry.inventory.entity.InventoryLog;
import br.com.hanrry.inventory.entity.Product;
import br.com.hanrry.inventory.entity.enums.LogType;
import br.com.hanrry.inventory.mapper.InventoryLogMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InventoryLogMapperTest {

    private final InventoryLogMapper inventoryLogMapper =
            Mappers.getMapper(InventoryLogMapper.class);

    @Test
    void shouldMapEntityToResponseDto() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Notebook");

        Batch batch = new Batch();
        batch.setId(10L);

        InventoryLog log = new InventoryLog();
        log.setId(1L);
        log.setProduct(product);
        log.setBatch(batch);
        log.setQuantity(5L);
        log.setType(LogType.INPUT);

        InventoryLogResponseDTO response = inventoryLogMapper.toDTO(log);

        assertNotNull(response);
        assertEquals("Notebook", response.productName());
        assertEquals(10L, response.batchId());
        assertEquals(5L, response.quantity());
        assertEquals(LogType.INPUT, response.type());
    }

    @Test
    void shouldMapEntityListToResponseDtoList() {
        Product product = new Product();
        product.setName("Notebook");

        Batch batch = new Batch();
        batch.setId(10L);

        InventoryLog log = new InventoryLog();
        log.setProduct(product);
        log.setBatch(batch);
        log.setQuantity(5L);
        log.setType(LogType.INPUT);

        List<InventoryLogResponseDTO> result =
                inventoryLogMapper.toDTOList(List.of(log));

        assertEquals(1, result.size());
        assertEquals("Notebook", result.get(0).productName());
        assertEquals(10L, result.get(0).batchId());
    }
}