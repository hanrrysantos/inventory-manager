package br.com.hanrry.inventory.product.controller;

import br.com.hanrry.inventory.product.controller.docs.CategoryControllerDocs;
import br.com.hanrry.inventory.product.dto.category.CategoryRequestDTO;
import br.com.hanrry.inventory.product.dto.category.CategoryResponseDTO;
import br.com.hanrry.inventory.product.service.CategoryService;
import br.com.hanrry.inventory.shared.config.PaginationConfig;
import br.com.hanrry.inventory.shared.dto.PageResponse;
import br.com.hanrry.inventory.shared.pagination.PaginationBoundsValidator;
import br.com.hanrry.inventory.shared.pagination.PaginationSortValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController implements CategoryControllerDocs {

    private static final Set<String> CATEGORY_LIST_SORT_PROPERTIES = Set.of("id", "name");

    private final CategoryService categoryService;

    @GetMapping()
    public ResponseEntity<PageResponse<CategoryResponseDTO>> findAllCategories(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @PageableDefault(size = PaginationConfig.DEFAULT_PAGE_SIZE, sort = "name") Pageable pageable
    ) {
        PaginationBoundsValidator.validate(page, size, PaginationConfig.MAX_PAGE_SIZE);
        PaginationSortValidator.validateAllowedProperties(pageable, CATEGORY_LIST_SORT_PROPERTIES);
        PageResponse<CategoryResponseDTO> categoryPage = categoryService.findAllCategories(pageable);

        return ResponseEntity.ok().body(categoryPage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> findCategoryById(
            @PathVariable Long id
    ){
        CategoryResponseDTO category = categoryService.findCategoryById(id);

        return ResponseEntity.ok().body(category);
    }

    @PostMapping
    public ResponseEntity<CategoryResponseDTO> createCategory(
            @Valid @RequestBody CategoryRequestDTO request
    ){
        CategoryResponseDTO category = categoryService.createCategory(request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(category.id()).toUri();
        return ResponseEntity.created(uri).body(category);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequestDTO request
    ){
        CategoryResponseDTO category = categoryService.updateCategory(id, request);
        return ResponseEntity.ok().body(category);
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long id
    ){
        categoryService.deleteCategoryById(id);
        return ResponseEntity.noContent().build();
    }
}
