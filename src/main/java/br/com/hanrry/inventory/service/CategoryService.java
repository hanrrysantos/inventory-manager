package br.com.hanrry.inventory.service;

import br.com.hanrry.inventory.dto.category.CategoryRequestDTO;
import br.com.hanrry.inventory.dto.category.CategoryResponseDTO;
import br.com.hanrry.inventory.entity.Category;
import br.com.hanrry.inventory.exception.category.CascadeCategoryException;
import br.com.hanrry.inventory.exception.category.CategoryAlreadyExistsException;
import br.com.hanrry.inventory.exception.category.CategoryNotFoundException;
import br.com.hanrry.inventory.mapper.CategoryMapper;
import br.com.hanrry.inventory.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryResponseDTO createCategory(CategoryRequestDTO request){

        categoryRepository.findByNameIgnoreCase(request.name()).ifPresent(
                c -> {
                    throw new CategoryAlreadyExistsException("Category already exists");
                });

        Category category = categoryMapper.toEntity(request);

        Category savedCategory = categoryRepository.save(category);

        return categoryMapper.toDTO(savedCategory);
    }

    public Page<CategoryResponseDTO> findAllCategories(Pageable pageable){
        Page<Category> categories = categoryRepository.findAll(pageable);

        return categories.map(categoryMapper::toDTO);
    }

    public CategoryResponseDTO findCategoryById(Long id){
        Category category = categoryRepository.findById(id).orElseThrow(
                () -> new CategoryNotFoundException("Category not found with this id: " + id)
        );

        return categoryMapper.toDTO(category);
    }

    public CategoryResponseDTO updateCategory(Long id, CategoryRequestDTO request) {
        Category category = categoryRepository.findById(id).orElseThrow(
                () -> new CategoryNotFoundException("Category not found with this id: " + id)
        );

            if (request.description() != null && !request.description().isBlank()) {
                category.setDescription(request.description());
            }

            if (request.name() != null && !request.name().isBlank()) {
                category.setName(request.name());
            }

        Category savedCategory = categoryRepository.save(category);

        return categoryMapper.toDTO(savedCategory);
    }

    public void deleteCategoryById(Long id){
        Category category = categoryRepository.findById(id).orElseThrow(
                () -> new CategoryNotFoundException("Category not found with this id: " + id)
        );

        if (!category.getProducts().isEmpty()) {
            throw new CascadeCategoryException("This category cannot be deleted because it contains products");
        }
        categoryRepository.deleteById(id);
    }
}
