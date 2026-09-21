package br.com.hanrry.inventory.product.controllerTest;

import br.com.hanrry.inventory.product.controller.ProductController;
import br.com.hanrry.inventory.product.dto.product.ProductRequestDTO;
import br.com.hanrry.inventory.product.dto.product.ProductResponseDTO;
import br.com.hanrry.inventory.product.dto.product.UpdateProdcutRequestDTO;
import br.com.hanrry.inventory.product.service.ProductService;
import br.com.hanrry.inventory.shared.dto.PageResponse;
import br.com.hanrry.inventory.shared.exception.handler.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ProductController productController = new ProductController(productService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(productController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void shouldFindAllProducts() throws Exception {
        ProductResponseDTO product = new ProductResponseDTO(
                1L,
                "Notebook",
                "NOTE-001",
                5L,
                "Eletrônicos",
                10L
        );

        PageResponse<ProductResponseDTO> page = new PageResponse<>(
                List.of(product),
                0,
                20,
                1,
                1
        );

        when(productService.findAllProducts(any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].name").value("Notebook"))
                .andExpect(jsonPath("$.content[0].sku").value("NOTE-001"))
                .andExpect(jsonPath("$.content[0].totalQuantity").value(5L))
                .andExpect(jsonPath("$.content[0].categoryName").value("Eletrônicos"))
                .andExpect(jsonPath("$.content[0].minStock").value(10L))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(productService).findAllProducts(any(Pageable.class));
    }

    @Test
    void shouldRejectInvalidSortPropertyOnFindAllProducts() throws Exception {
        mockMvc.perform(get("/api/v1/products").param("sort", "category,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("InvalidPagination"));

        verifyNoInteractions(productService);
    }

    @Test
    void shouldRejectSizeAboveMaximumOnFindAllProducts() throws Exception {
        mockMvc.perform(get("/api/v1/products").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("InvalidPagination"));

        verifyNoInteractions(productService);
    }

    @Test
    void shouldRejectNestedSortPropertyOnFindAllProducts() throws Exception {
        mockMvc.perform(get("/api/v1/products").param("sort", "category.name,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("InvalidPagination"));

        verifyNoInteractions(productService);
    }

    @Test
    void shouldFindProductById() throws Exception {
        ProductResponseDTO product = new ProductResponseDTO(
                1L,
                "Notebook",
                "NOTE-001",
                5L,
                "Eletrônicos",
                10L
        );

        when(productService.findProductById(1L))
                .thenReturn(product);

        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Notebook"))
                .andExpect(jsonPath("$.sku").value("NOTE-001"));

        verify(productService).findProductById(1L);
    }

    @Test
    void shouldFindLowStockProducts() throws Exception {
        ProductResponseDTO product = new ProductResponseDTO(
                1L,
                "Notebook",
                "NOTE-001",
                3L,
                "Eletrônicos",
                10L
        );

        when(productService.getLowStockProducts())
                .thenReturn(List.of(product));

        mockMvc.perform(get("/api/v1/products/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Notebook"))
                .andExpect(jsonPath("$[0].totalQuantity").value(3L))
                .andExpect(jsonPath("$[0].minStock").value(10L));

        verify(productService).getLowStockProducts();
    }

    @Test
    void shouldCreateProduct() throws Exception {
        ProductRequestDTO request = new ProductRequestDTO(
                "Notebook",
                "NOTE-001",
                10L,
                1L
        );

        ProductResponseDTO response = new ProductResponseDTO(
                1L,
                "Notebook",
                "NOTE-001",
                0L,
                "Eletrônicos",
                10L
        );

        when(productService.createProduct(request))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/products")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/products/1"))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Notebook"));

        verify(productService).createProduct(request);
    }

    @Test
    void shouldUpdateProduct() throws Exception {
        UpdateProdcutRequestDTO request = new UpdateProdcutRequestDTO(
                "Notebook Gamer",
                15L
        );

        ProductResponseDTO response = new ProductResponseDTO(
                1L,
                "Notebook Gamer",
                "NOTE-001",
                5L,
                "Eletrônicos",
                15L
        );

        when(productService.updateProduct(1L, request))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/products/1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Notebook Gamer"))
                .andExpect(jsonPath("$.minStock").value(15L));

        verify(productService).updateProduct(1L, request);
    }

    @Test
    void shouldDeleteProduct() throws Exception {
        doNothing().when(productService).deleteProductById(1L);

        mockMvc.perform(delete("/api/v1/products/1"))
                .andExpect(status().isNoContent());

        verify(productService).deleteProductById(1L);
    }
}
