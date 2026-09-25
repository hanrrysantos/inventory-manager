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
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

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
    void shouldReturnUnauthorizedErrorWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", admin.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/api/v1/users/" + admin.getId()));
    }

    @Test
    void shouldRejectUserRoleAccessingUserEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", admin.getId())
                        .header(AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
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
    void shouldReturnCurrentAuthenticatedUserWithRole() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header(AUTHORIZATION, bearerToken(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(admin.getId()))
                .andExpect(jsonPath("$.email").value(admin.getEmail()))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void shouldAllowConfiguredFrontendOriginOnPreflight() throws Exception {
        mockMvc.perform(options("/api/v1/products")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void shouldNotAuthorizeUnknownFrontendOriginOnPreflight() throws Exception {
        mockMvc.perform(options("/api/v1/products")
                        .header("Origin", "https://unknown.example.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void shouldReturnValidationErrorForInvalidRegistration() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"invalid\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("ValidationError"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/register"));
    }

    @Test
    void shouldReturnUnauthorizedErrorForInvalidCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"email\":\"security-admin@example.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("InvalidCredentials"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/login"));
    }

    @Test
    void shouldRequireJwtToLinkGoogleAccount() throws Exception {
        mockMvc.perform(post("/api/v1/auth/google/link")
                        .contentType(APPLICATION_JSON)
                        .content("{\"idToken\":\"google-id-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
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
