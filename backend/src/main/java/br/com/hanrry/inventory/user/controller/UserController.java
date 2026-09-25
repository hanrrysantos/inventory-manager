package br.com.hanrry.inventory.user.controller;

import br.com.hanrry.inventory.user.controller.docs.UserControllerDocs;
import br.com.hanrry.inventory.user.dto.UpdateUserRequestDTO;
import br.com.hanrry.inventory.user.dto.UserResponseDTO;
import br.com.hanrry.inventory.user.service.UserService;
import br.com.hanrry.inventory.shared.config.PaginationConfig;
import br.com.hanrry.inventory.shared.dto.PageResponse;
import br.com.hanrry.inventory.shared.pagination.PaginationBoundsValidator;
import br.com.hanrry.inventory.shared.pagination.PaginationSortValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.Set;

@RestController
@RequestMapping(value = "/api/v1/users")
@RequiredArgsConstructor
public class UserController implements UserControllerDocs {

    private static final Set<String> USER_LIST_SORT_PROPERTIES = Set.of("id", "name", "email");

    private final UserService userService;

    @GetMapping
    public ResponseEntity<PageResponse<UserResponseDTO>> findAllUsers(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @PageableDefault(size = PaginationConfig.DEFAULT_PAGE_SIZE, sort = "name") Pageable pageable
    ) {
        PaginationBoundsValidator.validate(page, size, PaginationConfig.MAX_PAGE_SIZE);
        PaginationSortValidator.validateAllowedProperties(pageable, USER_LIST_SORT_PROPERTIES);
        return ResponseEntity.ok(userService.findAllUsers(pageable));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> findCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(userService.findUserByEmail(authentication.getName()));
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
            @Valid @RequestBody UpdateUserRequestDTO request
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
