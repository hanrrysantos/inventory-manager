package br.com.hanrry.inventory.controller;

import br.com.hanrry.inventory.dto.user.UpdateUserRequestDTO;
import br.com.hanrry.inventory.dto.user.UserResponseDTO;
import br.com.hanrry.inventory.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping(value = "/{id}")
    public ResponseEntity<UserResponseDTO> findUserById(
            @PathVariable Long id
    ){
        UserResponseDTO user = userService.findUserById(id);

        return ResponseEntity.ok().body(user);
    }

    @GetMapping
    public ResponseEntity<Page<UserResponseDTO>> findAllUsers(
            @PageableDefault(size = 3, sort = "id")
            Pageable pageable
    ){
        Page<UserResponseDTO> users = userService.findAllUsers(pageable);

        return ResponseEntity.ok().body(users);
    }

    @PutMapping(value = "/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(
            @PathVariable Long id,
            @RequestBody UpdateUserRequestDTO request
    ){
        UserResponseDTO user = userService.updateUser(id, request);

        return ResponseEntity.ok().body(user);
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> deleteUserById(
            @PathVariable Long id
    ){
        userService.deleteUserById(id);
        return ResponseEntity.noContent().build();
    }
}
