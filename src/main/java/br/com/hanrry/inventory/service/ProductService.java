package br.com.hanrry.inventory.service;

import br.com.hanrry.inventory.dto.product.ProductRequestDTO;
import br.com.hanrry.inventory.dto.product.ProductResponseDTO;
import br.com.hanrry.inventory.dto.product.UpdateProdcutRequestDTO;
import br.com.hanrry.inventory.entity.Category;
import br.com.hanrry.inventory.entity.Product;
import br.com.hanrry.inventory.exception.category.CategoryNotFoundException;
import br.com.hanrry.inventory.exception.product.ProductAlreadyExistsException;
import br.com.hanrry.inventory.exception.product.ProductNotFoundException;
import br.com.hanrry.inventory.mapper.ProductMapper;
import br.com.hanrry.inventory.repository.CategoryRepository;
import br.com.hanrry.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
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

    @Transactional
    public ProductResponseDTO createProduct(ProductRequestDTO request){

        productRepository.findBySku(request.sku()).ifPresent(
               p -> {
                   throw new ProductAlreadyExistsException("Product already exists");
               });
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new CategoryNotFoundException("Category not found"));

       Product product = productMapper.toEntity(request);

       product.setCategory(category);

       Product savedProduct = productRepository.save(product);

       return productMapper.toDTO(savedProduct);
    }

    public List<ProductResponseDTO> findAllProducts( ){
        List<Product> productsList = productRepository.findAll();

        return productMapper.toDTOList(productsList);
    }

    public ProductResponseDTO findProductById(Long id){
        Product product = productRepository.findById(id).orElseThrow(
                () -> new ProductNotFoundException("Product not found with this id: " + id)
        );

        return productMapper.toDTO(product);
    }

    @Transactional
    public ProductResponseDTO updateProduct(Long id, UpdateProdcutRequestDTO request){
        Product product = productRepository.findById(id).orElseThrow(
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

    public List<ProductResponseDTO> getLowStockProducts() {
        List<Product> allProducts = productRepository.findAll();

        return allProducts.stream()
                .filter(product -> {
                    Long totalStock = productMapper.calculateTotalQuantity(product);
                    return totalStock <= product.getMinStock();
                })
                .map(productMapper::toDTO)
                .toList();
    }
}
