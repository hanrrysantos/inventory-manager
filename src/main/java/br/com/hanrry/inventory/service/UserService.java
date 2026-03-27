package br.com.hanrry.inventory.service;

import br.com.hanrry.inventory.dto.user.UpdateUserRequestDTO;
import br.com.hanrry.inventory.dto.user.UserRequestDTO;
import br.com.hanrry.inventory.dto.user.UserResponseDTO;
import br.com.hanrry.inventory.entity.User;
import br.com.hanrry.inventory.entity.UserRole;
import br.com.hanrry.inventory.exception.user.EmailAlreadyExistsException;
import br.com.hanrry.inventory.exception.user.UserNotFoundException;
import br.com.hanrry.inventory.mapper.UserMapper;
import br.com.hanrry.inventory.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

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

    public Page<UserResponseDTO> findAllUsers(Pageable pageable){
        Page<User> users = userRepository.findAll(pageable);

        return users.map(userMapper::toDTO);
    }

    public UserResponseDTO updateUser(Long id, UpdateUserRequestDTO request){
        User user =  userRepository.findById(id).orElseThrow(
                () -> new UserNotFoundException("User not found with this id: " + id)
        );
        if(request.password() != null && !request.password().isBlank()){

            user.setPassword(request.password());
        }

        User savedUser = userRepository.save(user);

        return userMapper.toDTO(savedUser);
    }

    public void deleteUserById(Long id){
        findUserById(id);
        userRepository.deleteById(id);
    }
}
