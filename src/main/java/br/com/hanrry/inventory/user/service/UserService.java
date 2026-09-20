package br.com.hanrry.inventory.user.service;

import br.com.hanrry.inventory.auth.service.GoogleAuthService;
import br.com.hanrry.inventory.shared.exception.auth.GoogleAccountAlreadyLinkedException;
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
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final GoogleAuthService googleAuthService;

    public String authenticateWithGoogle(String idToken) {
        GoogleIdToken.Payload payload = googleAuthService.validarTokenGoogle(idToken);

        String googleSubject = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name");

        User user = userRepository.findByGoogleSubject(googleSubject)
                .orElseGet(() -> findOrCreateGoogleUser(googleSubject, email, name));

        return user.getEmail();
    }

    private User createGoogleUser(String googleSubject, String email, String name) {
        User user = new User();
        user.setName(name == null || name.isBlank() ? "Usuário Google" : name);
        user.setEmail(email);
        user.setGoogleSubject(googleSubject);
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setRole(UserRole.USER);
        user.setCreatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    private User findOrCreateGoogleUser(String googleSubject, String email, String name) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new GoogleAccountLinkRequiredException();
        }

        return createGoogleUser(googleSubject, email, name);
    }

    public void linkGoogleAccount(String currentUserEmail, String idToken) {
        GoogleIdToken.Payload payload = googleAuthService.validarTokenGoogle(idToken);
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with this email: " + currentUserEmail));

        userRepository.findByGoogleSubject(payload.getSubject())
                .filter(googleUser -> !googleUser.getEmail().equals(currentUserEmail))
                .ifPresent(googleUser -> {
                    throw new GoogleAccountAlreadyLinkedException();
                });

        currentUser.setGoogleSubject(payload.getSubject());
        userRepository.save(currentUser);
    }

    public UserResponseDTO createUser(UserRequestDTO request){

        if(userRepository.existsByEmail(request.email())){
            throw new EmailAlreadyExistsException("Email already exists");
        }

        User user = userMapper.toEntity(request);

        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.USER);
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        return userMapper.toDTO(savedUser);
    }

    public UserResponseDTO findUserById(Long id){
        User user =  userRepository.findById(id).orElseThrow(
                () -> new UserNotFoundException("User not found with this id: " + id)
        );

        return userMapper.toDTO(user);
    }

    public UserResponseDTO findUserByEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new UserNotFoundException("User not found with this email: " + email)
        );

        return userMapper.toDTO(user);
    }

    public List<UserResponseDTO> findAllUsers(){
        List<User> users = userRepository.findAll();

        return userMapper.toDTOList(users);
    }

    public UserResponseDTO updateUser(Long id, UpdateUserRequestDTO request){
        User user =  userRepository.findById(id).orElseThrow(
                () -> new UserNotFoundException("User not found with this id: " + id)
        );
        if(request.password() != null && !request.password().isBlank()){
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        User savedUser = userRepository.save(user);

        return userMapper.toDTO(savedUser);
    }

    public void deleteUserById(Long id){
        findUserById(id);
        userRepository.deleteById(id);
    }
}
