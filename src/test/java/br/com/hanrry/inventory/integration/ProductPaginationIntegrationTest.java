package br.com.hanrry.inventory.integration;

import br.com.hanrry.inventory.auth.security.JwtUtil;
import br.com.hanrry.inventory.user.entity.User;
import br.com.hanrry.inventory.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class ProductPaginationIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    private User admin;

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void setUpAdmin() {
        admin = userRepository.findByEmail("hanrry@email.com").orElseThrow();
    }

    @Test
    void shouldReturnPaginatedProductsWithTotalQuantityFromBatches() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .header(AUTHORIZATION, "Bearer " + jwtUtil.generateToken(admin.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(13))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content[0].totalQuantity").isNumber())
                .andExpect(jsonPath("$.content[0].name").exists());
    }

    @Test
    void shouldRejectSizeAboveMaximum() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .param("size", "101")
                        .header(AUTHORIZATION, "Bearer " + jwtUtil.generateToken(admin.getEmail())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("InvalidPagination"));
    }
}
