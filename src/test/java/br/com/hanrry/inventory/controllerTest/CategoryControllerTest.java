package br.com.hanrry.inventory.controllerTest;

import br.com.hanrry.inventory.controller.CategoryController;
import br.com.hanrry.inventory.dto.category.CategoryRequestDTO;
import br.com.hanrry.inventory.dto.category.CategoryResponseDTO;
import br.com.hanrry.inventory.service.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        CategoryController categoryController = new CategoryController(categoryService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(categoryController)
                .build();
    }

    @Test
    void shouldFindAllCategories() throws Exception {
        CategoryResponseDTO category = new CategoryResponseDTO(
                1L,
                "Eletrônicos",
                "Produtos eletrônicos"
        );

        when(categoryService.findAllCategories())
                .thenReturn(List.of(category));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Eletrônicos"))
                .andExpect(jsonPath("$[0].description").value("Produtos eletrônicos"));

        verify(categoryService).findAllCategories();
    }

    @Test
    void shouldFindCategoryById() throws Exception {
        CategoryResponseDTO category = new CategoryResponseDTO(
                1L,
                "Eletrônicos",
                "Produtos eletrônicos"
        );

        when(categoryService.findCategoryById(1L))
                .thenReturn(category);

        mockMvc.perform(get("/api/v1/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Eletrônicos"));

        verify(categoryService).findCategoryById(1L);
    }

    @Test
    void shouldCreateCategory() throws Exception {
        CategoryRequestDTO request = new CategoryRequestDTO(
                "Eletrônicos",
                "Produtos eletrônicos"
        );

        CategoryResponseDTO response = new CategoryResponseDTO(
                1L,
                "Eletrônicos",
                "Produtos eletrônicos"
        );

        when(categoryService.createCategory(request))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/categories")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/categories/1"))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Eletrônicos"));

        verify(categoryService).createCategory(request);
    }

    @Test
    void shouldUpdateCategory() throws Exception {
        CategoryRequestDTO request = new CategoryRequestDTO(
                "Periféricos",
                "Produtos periféricos"
        );

        CategoryResponseDTO response = new CategoryResponseDTO(
                1L,
                "Periféricos",
                "Produtos periféricos"
        );

        when(categoryService.updateCategory(1L, request))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/categories/1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Periféricos"))
                .andExpect(jsonPath("$.description").value("Produtos periféricos"));

        verify(categoryService).updateCategory(1L, request);
    }

    @Test
    void shouldDeleteCategory() throws Exception {
        doNothing().when(categoryService).deleteCategoryById(1L);

        mockMvc.perform(delete("/api/v1/categories/1"))
                .andExpect(status().isNoContent());

        verify(categoryService).deleteCategoryById(1L);
    }
}