package br.com.hanrry.inventory.serviceTest;

import br.com.hanrry.inventory.dto.category.CategoryRequestDTO;
import br.com.hanrry.inventory.dto.category.CategoryResponseDTO;
import br.com.hanrry.inventory.entity.Category;
import br.com.hanrry.inventory.entity.Product;
import br.com.hanrry.inventory.exception.category.CascadeCategoryException;
import br.com.hanrry.inventory.exception.category.CategoryAlreadyExistsException;
import br.com.hanrry.inventory.exception.category.CategoryNotFoundException;
import br.com.hanrry.inventory.mapper.CategoryMapper;
import br.com.hanrry.inventory.repository.CategoryRepository;
import br.com.hanrry.inventory.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void shouldCreateCategorySuccessfully() {
        CategoryRequestDTO request = new CategoryRequestDTO(
                "Eletrônicos",
                "Produtos eletrônicos"
        );

        Category category = new Category();
        category.setName("Eletrônicos");
        category.setDescription("Produtos eletrônicos");

        Category savedCategory = new Category();
        savedCategory.setId(1L);
        savedCategory.setName("Eletrônicos");
        savedCategory.setDescription("Produtos eletrônicos");

        CategoryResponseDTO response = new CategoryResponseDTO(
                1L,
                "Eletrônicos",
                "Produtos eletrônicos"
        );

        when(categoryRepository.findByNameIgnoreCase(request.name()))
                .thenReturn(Optional.empty());

        when(categoryMapper.toEntity(request))
                .thenReturn(category);

        when(categoryRepository.save(category))
                .thenReturn(savedCategory);

        when(categoryMapper.toDTO(savedCategory))
                .thenReturn(response);

        CategoryResponseDTO result = categoryService.createCategory(request);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("Eletrônicos", result.name());
        assertEquals("Produtos eletrônicos", result.description());

        verify(categoryRepository).findByNameIgnoreCase("Eletrônicos");
        verify(categoryMapper).toEntity(request);
        verify(categoryRepository).save(category);
        verify(categoryMapper).toDTO(savedCategory);
    }

    @Test
    void shouldThrowExceptionWhenCategoryAlreadyExists() {
        CategoryRequestDTO request = new CategoryRequestDTO(
                "Eletrônicos",
                "Produtos eletrônicos"
        );

        Category existingCategory = new Category();
        existingCategory.setId(1L);
        existingCategory.setName("Eletrônicos");

        when(categoryRepository.findByNameIgnoreCase(request.name()))
                .thenReturn(Optional.of(existingCategory));

        assertThrows(
                CategoryAlreadyExistsException.class,
                () -> categoryService.createCategory(request)
        );

        verify(categoryRepository).findByNameIgnoreCase("Eletrônicos");
        verifyNoInteractions(categoryMapper);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void shouldFindAllCategoriesSuccessfully() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Eletrônicos");
        category.setDescription("Produtos eletrônicos");

        CategoryResponseDTO response = new CategoryResponseDTO(
                1L,
                "Eletrônicos",
                "Produtos eletrônicos"
        );

        List<Category> categories = List.of(category);
        List<CategoryResponseDTO> responses = List.of(response);

        when(categoryRepository.findAll())
                .thenReturn(categories);

        when(categoryMapper.toDTOList(categories))
                .thenReturn(responses);

        List<CategoryResponseDTO> result = categoryService.findAllCategories();

        assertEquals(1, result.size());
        assertEquals("Eletrônicos", result.get(0).name());

        verify(categoryRepository).findAll();
        verify(categoryMapper).toDTOList(categories);
    }

    @Test
    void shouldFindCategoryByIdSuccessfully() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Eletrônicos");
        category.setDescription("Produtos eletrônicos");

        CategoryResponseDTO response = new CategoryResponseDTO(
                1L,
                "Eletrônicos",
                "Produtos eletrônicos"
        );

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(category));

        when(categoryMapper.toDTO(category))
                .thenReturn(response);

        CategoryResponseDTO result = categoryService.findCategoryById(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("Eletrônicos", result.name());

        verify(categoryRepository).findById(1L);
        verify(categoryMapper).toDTO(category);
    }

    @Test
    void shouldThrowExceptionWhenCategoryNotFoundById() {
        when(categoryRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                CategoryNotFoundException.class,
                () -> categoryService.findCategoryById(99L)
        );

        verify(categoryRepository).findById(99L);
        verifyNoInteractions(categoryMapper);
    }

    @Test
    void shouldUpdateCategorySuccessfully() {
        CategoryRequestDTO request = new CategoryRequestDTO(
                "Periféricos",
                "Produtos periféricos"
        );

        Category category = new Category();
        category.setId(1L);
        category.setName("Eletrônicos");
        category.setDescription("Produtos eletrônicos");

        Category savedCategory = new Category();
        savedCategory.setId(1L);
        savedCategory.setName("Periféricos");
        savedCategory.setDescription("Produtos periféricos");

        CategoryResponseDTO response = new CategoryResponseDTO(
                1L,
                "Periféricos",
                "Produtos periféricos"
        );

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(category));

        when(categoryRepository.save(category))
                .thenReturn(savedCategory);

        when(categoryMapper.toDTO(savedCategory))
                .thenReturn(response);

        CategoryResponseDTO result = categoryService.updateCategory(1L, request);

        assertNotNull(result);
        assertEquals("Periféricos", result.name());
        assertEquals("Produtos periféricos", result.description());

        verify(categoryRepository).findById(1L);
        verify(categoryRepository).save(category);
        verify(categoryMapper).toDTO(savedCategory);
    }

    @Test
    void shouldNotUpdateCategoryWhenFieldsAreNullOrBlank() {
        CategoryRequestDTO request = new CategoryRequestDTO(
                "",
                null
        );

        Category category = new Category();
        category.setId(1L);
        category.setName("Eletrônicos");
        category.setDescription("Produtos eletrônicos");

        Category savedCategory = new Category();
        savedCategory.setId(1L);
        savedCategory.setName("Eletrônicos");
        savedCategory.setDescription("Produtos eletrônicos");

        CategoryResponseDTO response = new CategoryResponseDTO(
                1L,
                "Eletrônicos",
                "Produtos eletrônicos"
        );

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(category));

        when(categoryRepository.save(category))
                .thenReturn(savedCategory);

        when(categoryMapper.toDTO(savedCategory))
                .thenReturn(response);

        CategoryResponseDTO result = categoryService.updateCategory(1L, request);

        assertEquals("Eletrônicos", result.name());
        assertEquals("Produtos eletrônicos", result.description());

        verify(categoryRepository).findById(1L);
        verify(categoryRepository).save(category);
        verify(categoryMapper).toDTO(savedCategory);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingCategoryNotFound() {
        CategoryRequestDTO request = new CategoryRequestDTO(
                "Periféricos",
                "Produtos periféricos"
        );

        when(categoryRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                CategoryNotFoundException.class,
                () -> categoryService.updateCategory(99L, request)
        );

        verify(categoryRepository).findById(99L);
        verify(categoryRepository, never()).save(any());
        verifyNoInteractions(categoryMapper);
    }

    @Test
    void shouldDeleteCategoryByIdSuccessfully() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Eletrônicos");
        category.setProducts(new ArrayList<>());

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(category));

        categoryService.deleteCategoryById(1L);

        verify(categoryRepository).findById(1L);
        verify(categoryRepository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingCategoryWithProducts() {
        Product product = new Product();

        Category category = new Category();
        category.setId(1L);
        category.setName("Eletrônicos");
        category.setProducts(List.of(product));

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(category));

        assertThrows(
                CascadeCategoryException.class,
                () -> categoryService.deleteCategoryById(1L)
        );

        verify(categoryRepository).findById(1L);
        verify(categoryRepository, never()).deleteById(anyLong());
    }

    @Test
    void shouldThrowExceptionWhenDeletingCategoryNotFound() {
        when(categoryRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                CategoryNotFoundException.class,
                () -> categoryService.deleteCategoryById(99L)
        );

        verify(categoryRepository).findById(99L);
        verify(categoryRepository, never()).deleteById(anyLong());
    }
}