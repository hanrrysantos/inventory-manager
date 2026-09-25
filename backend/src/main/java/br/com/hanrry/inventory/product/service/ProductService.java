package br.com.hanrry.inventory.product.service;

import br.com.hanrry.inventory.product.dto.product.ProductRequestDTO;
import br.com.hanrry.inventory.product.dto.product.ProductResponseDTO;
import br.com.hanrry.inventory.product.dto.product.UpdateProdcutRequestDTO;
import br.com.hanrry.inventory.product.entity.Category;
import br.com.hanrry.inventory.product.entity.Product;
import br.com.hanrry.inventory.shared.exception.product.category.CategoryNotFoundException;
import br.com.hanrry.inventory.shared.exception.product.product.ProductAlreadyExistsException;
import br.com.hanrry.inventory.shared.exception.product.product.ProductNotFoundException;
import br.com.hanrry.inventory.product.mapper.ProductMapper;
import br.com.hanrry.inventory.product.repository.CategoryRepository;
import br.com.hanrry.inventory.product.repository.ProductRepository;
import br.com.hanrry.inventory.shared.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import br.com.hanrry.inventory.shared.security.OwnerContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final CategoryRepository categoryRepository;
    private final OwnerContext ownerContext;

    @Transactional
    public ProductResponseDTO createProduct(ProductRequestDTO request){

        var owner = ownerContext == null ? null : ownerContext.currentUser();
        (owner == null ? productRepository.findBySku(request.sku()) : productRepository.findBySkuAndOwner(request.sku(), owner)).ifPresent(
               p -> {
                   throw new ProductAlreadyExistsException("Product already exists");
               });
        Category category = (owner == null ? categoryRepository.findById(request.categoryId()) : categoryRepository.findByIdAndOwner(request.categoryId(), owner))
                .orElseThrow(() -> new CategoryNotFoundException("Category not found"));

       Product product = productMapper.toEntity(request);

       product.setOwner(owner);

       product.setCategory(category);

       Product savedProduct = productRepository.save(product);

       return productMapper.toDTO(savedProduct);
    }

    public PageResponse<ProductResponseDTO> findAllProducts(Pageable pageable) {
        var owner = ownerContext == null ? null : ownerContext.currentUser();
        Page<Product> page = owner == null
                ? productRepository.findAll(pageable)
                : productRepository.findAllByOwner(owner, pageable);

        return PageResponse.from(page, productMapper::toDTO);
    }

    public ProductResponseDTO findProductById(Long id){
        var owner = ownerContext == null ? null : ownerContext.currentUser();
        Product product = (owner == null ? productRepository.findById(id) : productRepository.findByIdAndOwner(id, owner)).orElseThrow(
                () -> new ProductNotFoundException("Product not found with this id: " + id)
        );

        return productMapper.toDTO(product);
    }

    @Transactional
    public ProductResponseDTO updateProduct(Long id, UpdateProdcutRequestDTO request){
        var owner = ownerContext == null ? null : ownerContext.currentUser();
        Product product = (owner == null ? productRepository.findById(id) : productRepository.findByIdAndOwner(id, owner)).orElseThrow(
                () -> new ProductNotFoundException("Product not found with this id: " + id)
        );

        if(request.name() != null && !request.name().isBlank()) {

            product.setName(request.name());
        }

        if(request.minStock() != null && (request.minStock() >= 0)) {

            product.setMinStock(request.minStock());
        }

        Product savedProduct = productRepository.save(product);

        return productMapper.toDTO(savedProduct);
    }

    @Transactional
    public void deleteProductById(Long id){
        findProductById(id);
        productRepository.deleteById(id);
    }

    public PageResponse<ProductResponseDTO> findLowStockProducts(Pageable pageable) {
        var owner = ownerContext == null ? null : ownerContext.currentUser();
        Page<Product> page = productRepository.findLowStockProducts(owner, pageable);
        return PageResponse.from(page, productMapper::toDTO);
    }

    public List<ProductResponseDTO> getLowStockProducts() {
        return findLowStockProducts(Pageable.unpaged()).content();
    }
}
