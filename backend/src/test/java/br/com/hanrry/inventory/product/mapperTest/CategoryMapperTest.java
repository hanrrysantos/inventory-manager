package br.com.hanrry.inventory.product.mapperTest;

import br.com.hanrry.inventory.product.dto.category.CategoryRequestDTO;
import br.com.hanrry.inventory.product.dto.category.CategoryResponseDTO;
import br.com.hanrry.inventory.product.entity.Category;
import br.com.hanrry.inventory.product.mapper.CategoryMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CategoryMapperTest {

    private final CategoryMapper categoryMapper =
            Mappers.getMapper(CategoryMapper.class);

    @Test
    void shouldMapRequestDtoToEntity() {
        CategoryRequestDTO request = new CategoryRequestDTO(
                "Eletrônicos",
                "Produtos eletrônicos"
        );

        Category category = categoryMapper.toEntity(request);

        assertNotNull(category);
        assertNull(category.getId());
        assertEquals("Eletrônicos", category.getName());
        assertEquals("Produtos eletrônicos", category.getDescription());
    }

    @Test
    void shouldMapEntityToResponseDto() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Eletrônicos");
        category.setDescription("Produtos eletrônicos");

        CategoryResponseDTO response = categoryMapper.toDTO(category);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Eletrônicos", response.name());
        assertEquals("Produtos eletrônicos", response.description());
    }

    @Test
    void shouldMapEntityListToResponseDtoList() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Eletrônicos");
        category.setDescription("Produtos eletrônicos");

        List<CategoryResponseDTO> result = categoryMapper.toDTOList(List.of(category));

        assertEquals(1, result.size());
        assertEquals("Eletrônicos", result.get(0).name());
    }
}
