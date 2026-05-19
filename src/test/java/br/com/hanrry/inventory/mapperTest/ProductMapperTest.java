package br.com.hanrry.inventory.mapperTest;

import br.com.hanrry.inventory.dto.product.ProductRequestDTO;
import br.com.hanrry.inventory.dto.product.ProductResponseDTO;
import br.com.hanrry.inventory.entity.Batch;
import br.com.hanrry.inventory.entity.Category;
import br.com.hanrry.inventory.entity.Product;
import br.com.hanrry.inventory.mapper.ProductMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductMapperTest {

    private final ProductMapper productMapper =
            Mappers.getMapper(ProductMapper.class);

    @Test
    void shouldMapRequestDtoToEntity() {
        ProductRequestDTO request = new ProductRequestDTO(
                "Notebook",
                "NOTE-001",
                10L,
                1L
        );

        Product product = productMapper.toEntity(request);

        assertNotNull(product);
        assertNull(product.getId());
        assertEquals("Notebook", product.getName());
        assertEquals("NOTE-001", product.getSku());
        assertEquals(10L, product.getMinStock());
    }

    @Test
    void shouldMapEntityToResponseDto() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Eletrônicos");

        Batch batch1 = new Batch();
        batch1.setQuantity(5L);

        Batch batch2 = new Batch();
        batch2.setQuantity(7L);

        Product product = new Product();
        product.setId(1L);
        product.setName("Notebook");
        product.setSku("NOTE-001");
        product.setMinStock(10L);
        product.setCategory(category);
        product.setBatches(List.of(batch1, batch2));

        ProductResponseDTO response = productMapper.toDTO(product);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Notebook", response.name());
        assertEquals("NOTE-001", response.sku());
        assertEquals(12L, response.totalQuantity());
        assertEquals("Eletrônicos", response.categoryName());
        assertEquals(10L, response.minStock());
    }

    @Test
    void shouldReturnZeroWhenProductBatchesIsNull() {
        Product product = new Product();
        product.setBatches(null);

        Long total = productMapper.calculateTotalQuantity(product);

        assertEquals(0L, total);
    }

    @Test
    void shouldIgnoreNullBatchQuantityWhenCalculatingTotalQuantity() {
        Batch batch1 = new Batch();
        batch1.setQuantity(5L);

        Batch batch2 = new Batch();
        batch2.setQuantity(null);

        Product product = new Product();
        product.setBatches(List.of(batch1, batch2));

        Long total = productMapper.calculateTotalQuantity(product);

        assertEquals(5L, total);
    }

    @Test
    void shouldMapEntityListToResponseDtoList() {
        Category category = new Category();
        category.setName("Eletrônicos");

        Product product = new Product();
        product.setId(1L);
        product.setName("Notebook");
        product.setSku("NOTE-001");
        product.setMinStock(10L);
        product.setCategory(category);
        product.setBatches(List.of());

        List<ProductResponseDTO> result = productMapper.toDTOList(List.of(product));

        assertEquals(1, result.size());
        assertEquals("Notebook", result.get(0).name());
    }
}