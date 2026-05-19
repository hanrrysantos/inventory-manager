package br.com.hanrry.inventory.serviceTest;

import br.com.hanrry.inventory.dto.user.UpdateUserRequestDTO;
import br.com.hanrry.inventory.dto.user.UserRequestDTO;
import br.com.hanrry.inventory.dto.user.UserResponseDTO;
import br.com.hanrry.inventory.entity.User;
import br.com.hanrry.inventory.exception.user.EmailAlreadyExistsException;
import br.com.hanrry.inventory.exception.user.UserNotFoundException;
import br.com.hanrry.inventory.mapper.UserMapper;
import br.com.hanrry.inventory.repository.UserRepository;
import br.com.hanrry.inventory.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User user;
    private UserRequestDTO userRequestDTO;
    private UserResponseDTO userResponseDTO;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("hanrry@gmail.com");
        user.setPassword("123456");

        userRequestDTO = new UserRequestDTO(
                "Hanrry",
                "hanrry@gmail.com",
                "123456"
        );

        userResponseDTO = new UserResponseDTO(
                1L,
                "Hanrry",
                "hanrry@gmail.com",
                LocalDateTime.now()
        );
    }

    @Test
    void shouldCreateUserSuccessfully() {

        when(userRepository.existsByEmail(userRequestDTO.email())).thenReturn(false);
        when(userMapper.toEntity(userRequestDTO)).thenReturn(user);
        when(passwordEncoder.encode(userRequestDTO.password())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(userResponseDTO);

        UserResponseDTO response = userService.createUser(userRequestDTO);

        assertNotNull(response);
        assertEquals(userResponseDTO.email(), response.email());

        verify(userRepository).existsByEmail(userRequestDTO.email());
        verify(passwordEncoder).encode(userRequestDTO.password());
        verify(userRepository).save(user);
    }

    @Test
    void shouldThrowExceptionWhenEmailAlreadyExists() {

        when(userRepository.existsByEmail(userRequestDTO.email())).thenReturn(true);

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> userService.createUser(userRequestDTO)
        );

        verify(userRepository).existsByEmail(userRequestDTO.email());
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldFindUserByIdSuccessfully() {

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toDTO(user)).thenReturn(userResponseDTO);

        UserResponseDTO response = userService.findUserById(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());

        verify(userRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenUserNotFoundById() {

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> userService.findUserById(1L)
        );

        verify(userRepository).findById(1L);
    }

    @Test
    void shouldFindAllUsersSuccessfully() {

        List<User> users = List.of(user);
        List<UserResponseDTO> responseDTOList = List.of(userResponseDTO);

        when(userRepository.findAll()).thenReturn(users);
        when(userMapper.toDTOList(users)).thenReturn(responseDTOList);

        List<UserResponseDTO> response = userService.findAllUsers();

        assertEquals(1, response.size());

        verify(userRepository).findAll();
    }

    @Test
    void shouldUpdateUserSuccessfully() {

        UpdateUserRequestDTO request =
                new UpdateUserRequestDTO("newPassword");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(userResponseDTO);

        UserResponseDTO response = userService.updateUser(1L, request);

        assertNotNull(response);

        verify(userRepository).save(user);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonexistentUser() {

        UpdateUserRequestDTO request =
                new UpdateUserRequestDTO("newPassword");

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> userService.updateUser(1L, request)
        );

        verify(userRepository).findById(1L);
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldDeleteUserSuccessfully() {

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toDTO(user)).thenReturn(userResponseDTO);

        userService.deleteUserById(1L);

        verify(userRepository).deleteById(1L);
    }
}