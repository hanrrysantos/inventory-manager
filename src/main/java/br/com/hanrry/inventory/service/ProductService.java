package br.com.hanrry.inventory.service;

import br.com.hanrry.inventory.dto.product.ProductRequestDTO;
import br.com.hanrry.inventory.dto.product.ProductResponseDTO;
import br.com.hanrry.inventory.dto.product.UpdateProdcutRequestDTO;
import br.com.hanrry.inventory.entity.Product;
import br.com.hanrry.inventory.exception.product.ProductAlreadyExistsException;
import br.com.hanrry.inventory.exception.product.ProductNotFoundException;
import br.com.hanrry.inventory.mapper.ProductMapper;
import br.com.hanrry.inventory.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    public ProductService(ProductRepository productRepository, ProductMapper productMapper){
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    public ProductResponseDTO createProduct(ProductRequestDTO request){

        productRepository.findBySku(request.sku()).ifPresent(
               p -> {
                   throw new ProductAlreadyExistsException("Product already exists");
               });
       Product product = productMapper.toEntity(request);

       Product savedProduct = productRepository.save(product);

       return productMapper.toDTO(savedProduct);
    }

    public Page<ProductResponseDTO> findAllProducts(Pageable pageable){
        Page<Product> productPage = productRepository.findAll(pageable);

        return productPage.map(productMapper::toDTO);
    }

    public ProductResponseDTO findProductById(Long id){
        Product product = productRepository.findById(id).orElseThrow(
                () -> new ProductNotFoundException("Product not found with this id: " + id)
        );

        return productMapper.toDTO(product);
    }

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
