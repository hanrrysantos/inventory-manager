package br.com.hanrry.inventory.controllerTest;

import br.com.hanrry.inventory.controller.AuthController;
import br.com.hanrry.inventory.dto.auth.AuthRequestDTO;
import br.com.hanrry.inventory.dto.user.UserRequestDTO;
import br.com.hanrry.inventory.dto.user.UserResponseDTO;
import br.com.hanrry.inventory.security.JwtUtil;
import br.com.hanrry.inventory.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        AuthController authController = new AuthController(
                userService,
                authenticationManager,
                jwtUtil
        );

        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .build();
    }

    @Test
    void shouldLoginUserSuccessfully() throws Exception {
        AuthRequestDTO request = new AuthRequestDTO(
                "admin@email.com",
                "admin123"
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);

        when(jwtUtil.generateToken(request.email()))
                .thenReturn("fake-jwt-token");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fake-jwt-token"));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil).generateToken("admin@email.com");
    }

    @Test
    void shouldRegisterUserSuccessfully() throws Exception {
        UserRequestDTO request = new UserRequestDTO(
                "Hanrry",
                "hanrry@email.com",
                "123456"
        );

        UserResponseDTO response = new UserResponseDTO(
                1L,
                "Hanrry",
                "hanrry@email.com",
                LocalDateTime.of(2026, 5, 19, 8, 30)
        );

        when(userService.createUser(request))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/auth/register/1"))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Hanrry"))
                .andExpect(jsonPath("$.email").value("hanrry@email.com"));

        verify(userService).createUser(request);
    }
}