package br.com.hanrry.inventory.user.controller;

import br.com.hanrry.inventory.user.controller.docs.UserControllerDocs;
import br.com.hanrry.inventory.user.dto.UpdateUserRequestDTO;
import br.com.hanrry.inventory.user.dto.UserResponseDTO;
import br.com.hanrry.inventory.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/users")
@RequiredArgsConstructor
public class UserController implements UserControllerDocs {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> findAllUsers(
    ){
        List<UserResponseDTO> users = userService.findAllUsers();

        return ResponseEntity.ok().body(users);
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<UserResponseDTO> findUserById(
            @PathVariable Long id
    ){
        UserResponseDTO user = userService.findUserById(id);

        return ResponseEntity.ok().body(user);
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
