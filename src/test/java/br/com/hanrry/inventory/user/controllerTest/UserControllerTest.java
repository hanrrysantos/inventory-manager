package br.com.hanrry.inventory.user.controllerTest;

import br.com.hanrry.inventory.user.controller.UserController;
import br.com.hanrry.inventory.user.dto.UpdateUserRequestDTO;
import br.com.hanrry.inventory.user.dto.UserResponseDTO;
import br.com.hanrry.inventory.user.service.UserService;
import br.com.hanrry.inventory.shared.dto.PageResponse;
import br.com.hanrry.inventory.shared.exception.handler.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        UserController userController = new UserController(userService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void shouldFindAllUsers() throws Exception {
        UserResponseDTO user = new UserResponseDTO(
                1L,
                "Hanrry",
                "hanrry@email.com",
                LocalDateTime.of(2026, 5, 19, 8, 0)
        );

        PageResponse<UserResponseDTO> page = new PageResponse<>(
                List.of(user),
                0,
                20,
                1,
                1
        );

        when(userService.findAllUsers(any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].name").value("Hanrry"))
                .andExpect(jsonPath("$.content[0].email").value("hanrry@email.com"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(userService).findAllUsers(any(Pageable.class));
    }

    @Test
    void shouldFindUserById() throws Exception {
        UserResponseDTO user = new UserResponseDTO(
                1L,
                "Hanrry",
                "hanrry@email.com",
                LocalDateTime.of(2026, 5, 19, 8, 0)
        );

        when(userService.findUserById(1L))
                .thenReturn(user);

        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Hanrry"))
                .andExpect(jsonPath("$.email").value("hanrry@email.com"));

        verify(userService).findUserById(1L);
    }

    @Test
    void shouldUpdateUser() throws Exception {
        UpdateUserRequestDTO request = new UpdateUserRequestDTO(
                "newPassword123"
        );

        UserResponseDTO response = new UserResponseDTO(
                1L,
                "Hanrry",
                "hanrry@email.com",
                LocalDateTime.of(2026, 5, 19, 8, 0)
        );

        when(userService.updateUser(1L, request))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/users/1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Hanrry"))
                .andExpect(jsonPath("$.email").value("hanrry@email.com"));

        verify(userService).updateUser(1L, request);
    }

    @Test
    void shouldDeleteUserById() throws Exception {
        doNothing().when(userService).deleteUserById(1L);

        mockMvc.perform(delete("/api/v1/users/1"))
                .andExpect(status().isNoContent());

        verify(userService).deleteUserById(1L);
    }
}
