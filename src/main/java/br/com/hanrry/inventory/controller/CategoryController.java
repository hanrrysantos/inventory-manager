package br.com.hanrry.inventory.controller;

import br.com.hanrry.inventory.controller.docs.CategoryControllerDocs;
import br.com.hanrry.inventory.dto.category.CategoryRequestDTO;
import br.com.hanrry.inventory.dto.category.CategoryResponseDTO;
import br.com.hanrry.inventory.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController implements CategoryControllerDocs {

    private final CategoryService categoryService;

    @GetMapping()
    public ResponseEntity<List<CategoryResponseDTO>> findAllCategories(){
        List<CategoryResponseDTO> categoryList = categoryService.findAllCategories();

        return ResponseEntity.ok().body(categoryList);
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
            @RequestBody CategoryRequestDTO request
    ){
        CategoryResponseDTO category = categoryService.createCategory(request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(category.id()).toUri();
        return ResponseEntity.created(uri).body(category);
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