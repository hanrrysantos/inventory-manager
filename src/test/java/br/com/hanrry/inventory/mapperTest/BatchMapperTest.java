package br.com.hanrry.inventory.mapperTest;

import br.com.hanrry.inventory.dto.batch.BatchRequestDTO;
import br.com.hanrry.inventory.dto.batch.BatchResponseDTO;
import br.com.hanrry.inventory.entity.Batch;
import br.com.hanrry.inventory.entity.Product;
import br.com.hanrry.inventory.mapper.BatchMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BatchMapperTest {

    private final BatchMapper batchMapper =
            Mappers.getMapper(BatchMapper.class);

    @Test
    void shouldMapRequestDtoToEntity() {
        LocalDate manufacturingDate = LocalDate.of(2025, 1, 10);
        LocalDate expiryDate = LocalDate.of(2026, 1, 10);

        BatchRequestDTO request = new BatchRequestDTO(
                "BATCH-001",
                10L,
                manufacturingDate,
                expiryDate,
                BigDecimal.valueOf(2500),
                1L
        );

        Batch batch = batchMapper.toEntity(request);

        assertNotNull(batch);
        assertNull(batch.getId());
        assertNull(batch.getProduct());
        assertEquals("BATCH-001", batch.getBatchNumber());
        assertEquals(10L, batch.getQuantity());
        assertEquals(manufacturingDate, batch.getManufacturingDate());
        assertEquals(expiryDate, batch.getExpiryDate());
        assertEquals(BigDecimal.valueOf(2500), batch.getPrice());
    }

    @Test
    void shouldMapEntityToResponseDto() {
        LocalDate manufacturingDate = LocalDate.of(2025, 1, 10);
        LocalDate expiryDate = LocalDate.of(2026, 1, 10);

        Product product = new Product();
        product.setId(1L);
        product.setName("Notebook");

        Batch batch = new Batch();
        batch.setId(1L);
        batch.setBatchNumber("BATCH-001");
        batch.setQuantity(10L);
        batch.setManufacturingDate(manufacturingDate);
        batch.setExpiryDate(expiryDate);
        batch.setPrice(BigDecimal.valueOf(2500));
        batch.setProduct(product);

        BatchResponseDTO response = batchMapper.toDTO(batch);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("BATCH-001", response.batchNumber());
        assertEquals(10L, response.quantity());
        assertEquals(manufacturingDate, response.manufacturingDate());
        assertEquals(expiryDate, response.expiryDate());
        assertEquals(BigDecimal.valueOf(2500), response.price());
        assertEquals(1L, response.productId());
        assertEquals("Notebook", response.productName());
    }

    @Test
    void shouldMapEntityListToResponseDtoList() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Notebook");

        Batch batch = new Batch();
        batch.setId(1L);
        batch.setBatchNumber("BATCH-001");
        batch.setQuantity(10L);
        batch.setProduct(product);

        List<BatchResponseDTO> result = batchMapper.toDTOList(List.of(batch));

        assertEquals(1, result.size());
        assertEquals("BATCH-001", result.get(0).batchNumber());
        assertEquals("Notebook", result.get(0).productName());
    }
}