package br.com.hanrry.inventory.user.serviceTest;

import br.com.hanrry.inventory.auth.service.GoogleAuthService;
import br.com.hanrry.inventory.shared.exception.auth.GoogleAccountLinkRequiredException;
import br.com.hanrry.inventory.user.dto.UpdateUserRequestDTO;
import br.com.hanrry.inventory.user.dto.UserRequestDTO;
import br.com.hanrry.inventory.user.dto.UserResponseDTO;
import br.com.hanrry.inventory.user.entity.User;
import br.com.hanrry.inventory.user.entity.enums.UserRole;
import br.com.hanrry.inventory.shared.exception.user.EmailAlreadyExistsException;
import br.com.hanrry.inventory.shared.exception.user.UserNotFoundException;
import br.com.hanrry.inventory.user.mapper.UserMapper;
import br.com.hanrry.inventory.user.repository.UserRepository;
import br.com.hanrry.inventory.user.service.UserService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import br.com.hanrry.inventory.shared.dto.PageResponse;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private GoogleAuthService googleAuthService;

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
    void shouldAuthenticateGoogleUserByStableGoogleSubject() {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload()
                .setSubject("google-subject-123")
                .setEmail("hanrry@gmail.com")
                .setEmailVerified(true)
                .set("name", "Hanrry");

        when(googleAuthService.validarTokenGoogle("google-id-token")).thenReturn(payload);
        when(userRepository.findByGoogleSubject("google-subject-123")).thenReturn(Optional.of(user));

        String authenticatedEmail = assertDoesNotThrow(
                () -> userService.authenticateWithGoogle("google-id-token")
        );

        assertEquals("hanrry@gmail.com", authenticatedEmail);
        verify(userRepository, never()).findByEmail("hanrry@gmail.com");
    }

    @Test
    void shouldCreateGoogleUserWithVerifiedIdentityWhenSubjectIsUnknown() {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload()
                .setSubject("google-subject-456")
                .setEmail("new.user@gmail.com")
                .setEmailVerified(true)
                .set("name", "New User");

        when(googleAuthService.validarTokenGoogle("google-id-token")).thenReturn(payload);
        when(userRepository.findByGoogleSubject("google-subject-456")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new.user@gmail.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-random-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String authenticatedEmail = assertDoesNotThrow(
                () -> userService.authenticateWithGoogle("google-id-token")
        );

        assertEquals("new.user@gmail.com", authenticatedEmail);
        verify(userRepository).findByEmail("new.user@gmail.com");
        verify(userRepository).save(argThat(savedUser ->
                "google-subject-456".equals(savedUser.getGoogleSubject())
                        && "new.user@gmail.com".equals(savedUser.getEmail())
                        && "New User".equals(savedUser.getName())
                        && UserRole.USER.equals(savedUser.getRole())
        ));
    }

    @Test
    void shouldRejectGoogleLoginForExistingAccountWithoutGoogleLink() {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload()
                .setSubject("google-subject-789")
                .setEmail("hanrry@gmail.com")
                .setEmailVerified(true)
                .set("name", "Hanrry");

        when(googleAuthService.validarTokenGoogle("google-id-token")).thenReturn(payload);
        when(userRepository.findByGoogleSubject("google-subject-789")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("hanrry@gmail.com")).thenReturn(Optional.of(user));

        assertThrows(GoogleAccountLinkRequiredException.class, () -> userService.authenticateWithGoogle("google-id-token"));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldLinkGoogleSubjectToAuthenticatedUser() {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload()
                .setSubject("google-subject-987")
                .setEmail("hanrry@gmail.com")
                .setEmailVerified(true);

        when(googleAuthService.validarTokenGoogle("google-id-token")).thenReturn(payload);
        when(userRepository.findByEmail("hanrry@gmail.com")).thenReturn(Optional.of(user));
        when(userRepository.findByGoogleSubject("google-subject-987")).thenReturn(Optional.empty());

        userService.linkGoogleAccount("hanrry@gmail.com", "google-id-token");

        assertEquals("google-subject-987", user.getGoogleSubject());
        verify(userRepository).save(user);
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

        Pageable pageable = PageRequest.of(0, 20);
        Page<User> userPage = new PageImpl<>(List.of(user), pageable, 1);

        when(userRepository.findAll(pageable)).thenReturn(userPage);
        when(userMapper.toDTO(user)).thenReturn(userResponseDTO);

        PageResponse<UserResponseDTO> response = userService.findAllUsers(pageable);

        assertEquals(1, response.content().size());
        verify(userRepository).findAll(pageable);
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
    void shouldEncodePasswordWhenUpdatingUser() {
        UpdateUserRequestDTO request = new UpdateUserRequestDTO("newPassword");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(request.password())).thenReturn("encodedNewPassword");
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(userResponseDTO);

        userService.updateUser(1L, request);

        assertEquals("encodedNewPassword", user.getPassword());
        verify(passwordEncoder).encode(request.password());
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
