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
class SecurityIntegrationTest {

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
    void setUpUsers() {
        admin = saveIfAbsent(
                "security-admin@example.com",
                "Security Admin",
                UserRole.ADMIN
        );
        user = saveIfAbsent(
                "security-user@example.com",
                "Security User",
                UserRole.USER
        );
    }

    @Test
    void shouldCharacterizeCurrentForbiddenResponseWithoutJwt() throws Exception {
        // Dívida de segurança: o plano desejava 401, mas o comportamento atual é 403.
        mockMvc.perform(get("/api/v1/users/{id}", admin.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectUserRoleAccessingUserEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", admin.getId())
                        .header(AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAdminAccessingUserEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", user.getId())
                        .header(AUTHORIZATION, bearerToken(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.email").value(user.getEmail()));
    }

    @Test
    void shouldAllowUnauthenticatedAccessToOpenApiDocs() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    private User saveIfAbsent(String email, String name, UserRole role) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setName(name);
            newUser.setEmail(email);
            newUser.setPassword("test-password");
            newUser.setRole(role);
            newUser.setCreatedAt(LocalDateTime.now());
            return userRepository.save(newUser);
        });
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtUtil.generateToken(user.getEmail());
    }
}
