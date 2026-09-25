package br.com.hanrry.inventory.product.serviceTest;

import br.com.hanrry.inventory.product.dto.category.CategoryRequestDTO;
import br.com.hanrry.inventory.product.dto.category.CategoryResponseDTO;
import br.com.hanrry.inventory.product.entity.Category;
import br.com.hanrry.inventory.product.entity.Product;
import br.com.hanrry.inventory.shared.exception.product.category.CascadeCategoryException;
import br.com.hanrry.inventory.shared.exception.product.category.CategoryAlreadyExistsException;
import br.com.hanrry.inventory.shared.exception.product.category.CategoryNotFoundException;
import br.com.hanrry.inventory.product.mapper.CategoryMapper;
import br.com.hanrry.inventory.product.repository.CategoryRepository;
import br.com.hanrry.inventory.product.service.CategoryService;
import br.com.hanrry.inventory.shared.dto.PageResponse;
import br.com.hanrry.inventory.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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

    @Mock
    private br.com.hanrry.inventory.shared.security.OwnerContext ownerContext;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void shouldRejectRenamingCategoryToAnotherCategoryOwnedBySameUser() {
        var owner = new br.com.hanrry.inventory.user.entity.User();
        owner.setId(7L);
        when(ownerContext.currentUser()).thenReturn(owner);

        Category current = new Category();
        current.setId(1L);
        current.setOwner(owner);
        when(categoryRepository.findByIdAndOwner(1L, owner)).thenReturn(Optional.of(current));

        Category duplicate = new Category();
        duplicate.setId(2L);
        when(categoryRepository.findByNameIgnoreCaseAndOwnerExcludingId("Bebidas", owner, 1L))
                .thenReturn(Optional.of(duplicate));

        assertThrows(CategoryAlreadyExistsException.class,
                () -> categoryService.updateCategory(1L, new CategoryRequestDTO("Bebidas", null)));
        verify(categoryRepository, never()).save(any());
    }

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

        Pageable pageable = PageRequest.of(0, 20);
        Page<Category> categoryPage = new PageImpl<>(List.of(category), pageable, 1);

        when(categoryRepository.findAll(pageable))
                .thenReturn(categoryPage);

        when(categoryMapper.toDTO(category))
                .thenReturn(response);

        PageResponse<CategoryResponseDTO> result = categoryService.findAllCategories(pageable);

        assertEquals(1, result.content().size());
        assertEquals("Eletrônicos", result.content().getFirst().name());
        assertEquals(0, result.page());
        assertEquals(20, result.size());
        assertEquals(1, result.totalElements());

        verify(categoryRepository).findAll(pageable);
        verify(categoryMapper).toDTO(category);
    }

    @Test
    void shouldFindAllCategoriesByOwnerWhenUserIsAuthenticated() {
        User owner = new User();
        owner.setId(7L);

        Category category = new Category();
        category.setId(1L);
        category.setName("Eletrônicos");

        CategoryResponseDTO response = new CategoryResponseDTO(
                1L,
                "Eletrônicos",
                "Produtos eletrônicos"
        );

        Pageable pageable = PageRequest.of(0, 20);
        Page<Category> categoryPage = new PageImpl<>(List.of(category), pageable, 1);

        when(ownerContext.currentUser()).thenReturn(owner);
        when(categoryRepository.findAllByOwner(owner, pageable)).thenReturn(categoryPage);
        when(categoryMapper.toDTO(category)).thenReturn(response);

        PageResponse<CategoryResponseDTO> result = categoryService.findAllCategories(pageable);

        assertEquals(1, result.content().size());
        verify(categoryRepository).findAllByOwner(owner, pageable);
        verify(categoryRepository, never()).findAll(any(Pageable.class));
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
