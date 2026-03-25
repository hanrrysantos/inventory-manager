package br.com.hanrry.inventory.controller;

import br.com.hanrry.inventory.dto.category.CategoryRequestDTO;
import br.com.hanrry.inventory.dto.category.CategoryResponseDTO;
import br.com.hanrry.inventory.dto.product.UpdateProdcutRequestDTO;
import br.com.hanrry.inventory.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CategoryResponseDTO> createCategory(
            @RequestBody CategoryRequestDTO request
    ){
        CategoryResponseDTO category = categoryService.createCategory(request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(category.id()).toUri();
        return ResponseEntity.created(uri).body(category);
    }


    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> findCategoryById(
            @PathVariable Long id
    ){
        CategoryResponseDTO category = categoryService.findCategoryById(id);

        return ResponseEntity.ok().body(category);
    }

    @GetMapping()
    public ResponseEntity<Page<CategoryResponseDTO>> findAllCategories(
            @PageableDefault(size = 3, sort = "id")
            Pageable pageable
    ){
        Page<CategoryResponseDTO> categoryList = categoryService.findAllCategories(pageable);

        return ResponseEntity.ok().body(categoryList);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> updateCategory(
            @PathVariable Long id,
            @RequestBody CategoryRequestDTO request
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