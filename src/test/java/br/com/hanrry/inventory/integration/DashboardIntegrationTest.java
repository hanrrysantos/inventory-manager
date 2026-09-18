package br.com.hanrry.inventory.integration;

import br.com.hanrry.inventory.auth.security.JwtUtil;
import br.com.hanrry.inventory.user.entity.User;
import br.com.hanrry.inventory.user.entity.enums.UserRole;
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

import java.time.LocalDateTime;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class DashboardIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    private User admin;
    private User user;

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void setUpUser() {
        admin = userRepository.findByEmail("dashboard-admin@example.com").orElseGet(() -> {
            User user = new User();
            user.setName("Dashboard Admin");
            user.setEmail("dashboard-admin@example.com");
            user.setPassword("test-password");
            user.setRole(UserRole.ADMIN);
            user.setCreatedAt(LocalDateTime.now());
            return userRepository.save(user);
        });
        user = userRepository.findByEmail("dashboard-user@example.com").orElseGet(() -> {
            User newUser = new User();
            newUser.setName("Dashboard User");
            newUser.setEmail("dashboard-user@example.com");
            newUser.setPassword("test-password");
            newUser.setRole(UserRole.USER);
            newUser.setCreatedAt(LocalDateTime.now());
            return userRepository.save(newUser);
        });
    }

    @Test
    void shouldReturnDashboardSummary() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/summary")
                        .header(AUTHORIZATION, "Bearer " + jwtUtil.generateToken(admin.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuantity").value(462))
                .andExpect(jsonPath("$.productCount").value(13))
                .andExpect(jsonPath("$.lowStockCount").value(4))
                .andExpect(jsonPath("$.outOfStockCount").value(0))
                .andExpect(jsonPath("$.expiredBatchCount").value(4))
                .andExpect(jsonPath("$.inventoryValue").value(3534.29))
                .andExpect(jsonPath("$.attentionItems[0].status").value("LOW_STOCK"));
    }

    @Test
    void shouldAllowUserRoleToReadDashboardSummary() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/summary")
                        .header(AUTHORIZATION, "Bearer " + jwtUtil.generateToken(user.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productCount").value(13));
    }
}
