package br.com.hanrry.inventory.product.service;

import br.com.hanrry.inventory.product.dto.category.CategoryRequestDTO;
import br.com.hanrry.inventory.product.dto.category.CategoryResponseDTO;
import br.com.hanrry.inventory.product.entity.Category;
import br.com.hanrry.inventory.shared.exception.product.category.CascadeCategoryException;
import br.com.hanrry.inventory.shared.exception.product.category.CategoryAlreadyExistsException;
import br.com.hanrry.inventory.shared.exception.product.category.CategoryNotFoundException;
import br.com.hanrry.inventory.product.mapper.CategoryMapper;
import br.com.hanrry.inventory.product.repository.CategoryRepository;
import br.com.hanrry.inventory.shared.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import br.com.hanrry.inventory.shared.security.OwnerContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final OwnerContext ownerContext;

    @Transactional
    public CategoryResponseDTO createCategory(CategoryRequestDTO request){

        var owner = ownerContext == null ? null : ownerContext.currentUser();
        (owner == null ? categoryRepository.findByNameIgnoreCase(request.name()) : categoryRepository.findByNameIgnoreCaseAndOwner(request.name(), owner)).ifPresent(
                c -> {
                    throw new CategoryAlreadyExistsException("Category already exists");
                });

        Category category = categoryMapper.toEntity(request);
        category.setOwner(owner);

        Category savedCategory = categoryRepository.save(category);

        return categoryMapper.toDTO(savedCategory);
    }

    public PageResponse<CategoryResponseDTO> findAllCategories(Pageable pageable) {
        var owner = ownerContext == null ? null : ownerContext.currentUser();
        Page<Category> page = owner == null
                ? categoryRepository.findAll(pageable)
                : categoryRepository.findAllByOwner(owner, pageable);

        return PageResponse.from(page, categoryMapper::toDTO);
    }

    public CategoryResponseDTO findCategoryById(Long id){
        var owner = ownerContext == null ? null : ownerContext.currentUser();
        Category category = (owner == null ? categoryRepository.findById(id) : categoryRepository.findByIdAndOwner(id, owner)).orElseThrow(
                () -> new CategoryNotFoundException("Category not found with this id: " + id)
        );

        return categoryMapper.toDTO(category);
    }

    @Transactional
    public CategoryResponseDTO updateCategory(Long id, CategoryRequestDTO request) {
        var owner = ownerContext == null ? null : ownerContext.currentUser();
        Category category = (owner == null ? categoryRepository.findById(id) : categoryRepository.findByIdAndOwner(id, owner)).orElseThrow(
                () -> new CategoryNotFoundException("Category not found with this id: " + id)
        );

        if (request.name() != null && !request.name().isBlank()) {
            (owner == null ? categoryRepository.findByNameIgnoreCase(request.name()) : categoryRepository.findByNameIgnoreCaseAndOwnerExcludingId(request.name(), owner, id)).ifPresent(existing -> {
                throw new CategoryAlreadyExistsException("Category already exists");
            });
        }

            if (request.description() != null && !request.description().isBlank()) {
                category.setDescription(request.description());
            }

            if (request.name() != null && !request.name().isBlank()) {
                category.setName(request.name());
            }

        Category savedCategory = categoryRepository.save(category);

        return categoryMapper.toDTO(savedCategory);
    }

    @Transactional
    public void deleteCategoryById(Long id){
        var owner = ownerContext == null ? null : ownerContext.currentUser();
        Category category = (owner == null ? categoryRepository.findById(id) : categoryRepository.findByIdAndOwner(id, owner)).orElseThrow(
                () -> new CategoryNotFoundException("Category not found with this id: " + id)
        );

        if (!category.getProducts().isEmpty()) {
            throw new CascadeCategoryException("This category cannot be deleted because it contains products");
        }
        categoryRepository.deleteById(id);
    }
}
