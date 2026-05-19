package br.com.hanrry.inventory.serviceTest;

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
import br.com.hanrry.inventory.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void shouldCreateProductSuccessfully() {
        ProductRequestDTO request = new ProductRequestDTO(
                "Notebook",
                "NOTE-001",
                10L,
                1L
        );

        Category category = new Category();
        category.setId(1L);
        category.setName("Eletrônicos");

        Product product = new Product();
        product.setName("Notebook");
        product.setSku("NOTE-001");
        product.setMinStock(10L);

        Product savedProduct = new Product();
        savedProduct.setId(1L);
        savedProduct.setName("Notebook");
        savedProduct.setSku("NOTE-001");
        savedProduct.setMinStock(10L);
        savedProduct.setCategory(category);

        ProductResponseDTO response = new ProductResponseDTO(
                1L,
                "Notebook",
                "NOTE-001",
                0L,
                "Eletrônicos",
                10L
        );

        when(productRepository.findBySku(request.sku()))
                .thenReturn(Optional.empty());

        when(categoryRepository.findById(request.categoryId()))
                .thenReturn(Optional.of(category));

        when(productMapper.toEntity(request))
                .thenReturn(product);

        when(productRepository.save(product))
                .thenReturn(savedProduct);

        when(productMapper.toDTO(savedProduct))
                .thenReturn(response);

        ProductResponseDTO result = productService.createProduct(request);

        assertNotNull(result);
        assertEquals("Notebook", result.name());
        assertEquals("NOTE-001", result.sku());
        assertEquals("Eletrônicos", result.categoryName());

        verify(productRepository).findBySku("NOTE-001");
        verify(categoryRepository).findById(1L);
        verify(productMapper).toEntity(request);
        verify(productRepository).save(product);
        verify(productMapper).toDTO(savedProduct);
    }

    @Test
    void shouldThrowExceptionWhenProductSkuAlreadyExists() {
        ProductRequestDTO request = new ProductRequestDTO(
                "Notebook",
                "NOTE-001",
                10L,
                1L
        );

        Product existingProduct = new Product();

        when(productRepository.findBySku(request.sku()))
                .thenReturn(Optional.of(existingProduct));

        assertThrows(
                ProductAlreadyExistsException.class,
                () -> productService.createProduct(request)
        );

        verify(productRepository).findBySku("NOTE-001");
        verifyNoInteractions(categoryRepository);
        verifyNoInteractions(productMapper);
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenCategoryNotFound() {
        ProductRequestDTO request = new ProductRequestDTO(
                "Notebook",
                "NOTE-001",
                10L,
                99L
        );

        when(productRepository.findBySku(request.sku()))
                .thenReturn(Optional.empty());

        when(categoryRepository.findById(request.categoryId()))
                .thenReturn(Optional.empty());

        assertThrows(
                CategoryNotFoundException.class,
                () -> productService.createProduct(request)
        );

        verify(productRepository).findBySku("NOTE-001");
        verify(categoryRepository).findById(99L);
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldFindAllProductsSuccessfully() {
        Product product = new Product();

        ProductResponseDTO response = new ProductResponseDTO(
                1L,
                "Notebook",
                "NOTE-001",
                5L,
                "Eletrônicos",
                10L
        );

        List<Product> products = List.of(product);
        List<ProductResponseDTO> responses = List.of(response);

        when(productRepository.findAll())
                .thenReturn(products);

        when(productMapper.toDTOList(products))
                .thenReturn(responses);

        List<ProductResponseDTO> result = productService.findAllProducts();

        assertEquals(1, result.size());
        assertEquals("Notebook", result.getFirst().name());

        verify(productRepository).findAll();
        verify(productMapper).toDTOList(products);
    }

    @Test
    void shouldFindProductByIdSuccessfully() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Notebook");

        ProductResponseDTO response = new ProductResponseDTO(
                1L,
                "Notebook",
                "NOTE-001",
                5L,
                "Eletrônicos",
                10L
        );

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(productMapper.toDTO(product))
                .thenReturn(response);

        ProductResponseDTO result = productService.findProductById(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("Notebook", result.name());

        verify(productRepository).findById(1L);
        verify(productMapper).toDTO(product);
    }

    @Test
    void shouldThrowExceptionWhenProductNotFoundById() {
        when(productRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> productService.findProductById(99L)
        );

        verify(productRepository).findById(99L);
        verifyNoInteractions(productMapper);
    }

    @Test
    void shouldUpdateProductSuccessfully() {
        UpdateProdcutRequestDTO request = new UpdateProdcutRequestDTO(
                "Notebook Gamer",
                15L
        );

        Product product = new Product();
        product.setId(1L);
        product.setName("Notebook");
        product.setMinStock(10L);

        Product savedProduct = new Product();
        savedProduct.setId(1L);
        savedProduct.setName("Notebook Gamer");
        savedProduct.setMinStock(15L);

        ProductResponseDTO response = new ProductResponseDTO(
                1L,
                "Notebook Gamer",
                "NOTE-001",
                5L,
                "Eletrônicos",
                15L
        );

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(productRepository.save(product))
                .thenReturn(savedProduct);

        when(productMapper.toDTO(savedProduct))
                .thenReturn(response);

        ProductResponseDTO result = productService.updateProduct(1L, request);

        assertEquals("Notebook Gamer", result.name());
        assertEquals(15L, result.minStock());

        verify(productRepository).findById(1L);
        verify(productRepository).save(product);
        verify(productMapper).toDTO(savedProduct);
    }

    @Test
    void shouldDeleteProductByIdSuccessfully() {
        Product product = new Product();
        product.setId(1L);

        ProductResponseDTO response = new ProductResponseDTO(
                1L,
                "Notebook",
                "NOTE-001",
                5L,
                "Eletrônicos",
                10L
        );

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(productMapper.toDTO(product))
                .thenReturn(response);

        productService.deleteProductById(1L);

        verify(productRepository).findById(1L);
        verify(productRepository).deleteById(1L);
    }

    @Test
    void shouldReturnLowStockProducts() {
        Product productLowStock = new Product();
        productLowStock.setId(1L);
        productLowStock.setName("Notebook");
        productLowStock.setMinStock(10L);

        Product productNormalStock = new Product();
        productNormalStock.setId(2L);
        productNormalStock.setName("Mouse");
        productNormalStock.setMinStock(5L);

        ProductResponseDTO response = new ProductResponseDTO(
                1L,
                "Notebook",
                "NOTE-001",
                3L,
                "Eletrônicos",
                10L
        );

        when(productRepository.findAll())
                .thenReturn(List.of(productLowStock, productNormalStock));

        when(productMapper.calculateTotalQuantity(productLowStock))
                .thenReturn(3L);

        when(productMapper.calculateTotalQuantity(productNormalStock))
                .thenReturn(20L);

        when(productMapper.toDTO(productLowStock))
                .thenReturn(response);

        List<ProductResponseDTO> result = productService.getLowStockProducts();

        assertEquals(1, result.size());
        assertEquals("Notebook", result.getFirst().name());

        verify(productRepository).findAll();
        verify(productMapper).calculateTotalQuantity(productLowStock);
        verify(productMapper).calculateTotalQuantity(productNormalStock);
        verify(productMapper).toDTO(productLowStock);
        verify(productMapper, never()).toDTO(productNormalStock);
    }
}