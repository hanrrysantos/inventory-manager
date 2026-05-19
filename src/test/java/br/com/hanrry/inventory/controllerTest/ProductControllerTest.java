package br.com.hanrry.inventory.controllerTest;

import br.com.hanrry.inventory.controller.ProductController;
import br.com.hanrry.inventory.dto.product.ProductRequestDTO;
import br.com.hanrry.inventory.dto.product.ProductResponseDTO;
import br.com.hanrry.inventory.dto.product.UpdateProdcutRequestDTO;
import br.com.hanrry.inventory.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

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

        when(productService.findAllProducts())
                .thenReturn(List.of(product));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Notebook"))
                .andExpect(jsonPath("$[0].sku").value("NOTE-001"))
                .andExpect(jsonPath("$[0].totalQuantity").value(5L))
                .andExpect(jsonPath("$[0].categoryName").value("Eletrônicos"))
                .andExpect(jsonPath("$[0].minStock").value(10L));

        verify(productService).findAllProducts();
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