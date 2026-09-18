package br.com.hanrry.inventory.auth.controller;

import br.com.hanrry.inventory.auth.controller.docs.AuthControllerDocs;
import br.com.hanrry.inventory.auth.dto.AuthRequestDTO;
import br.com.hanrry.inventory.auth.dto.AuthResponseDTO;
import br.com.hanrry.inventory.auth.security.JwtUtil;
import br.com.hanrry.inventory.user.dto.UserRequestDTO;
import br.com.hanrry.inventory.user.dto.UserResponseDTO;
import br.com.hanrry.inventory.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import jakarta.validation.Valid;

import java.net.URI;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/api/v1/auth")
public class AuthController implements AuthControllerDocs {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @PostMapping(value = "/login")
    public ResponseEntity<AuthResponseDTO> loginUser(@Valid @RequestBody AuthRequestDTO request){
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()
                ));

        String token = jwtUtil.generateToken(request.email());
        return ResponseEntity.ok().body(new AuthResponseDTO(token));
    }

    @PostMapping(value = "/register")
    public ResponseEntity<UserResponseDTO> registerUser(@Valid @RequestBody UserRequestDTO request){
        UserResponseDTO user = userService.createUser(request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(user.id()).toUri();

        return ResponseEntity.created(uri).body(user);
    }
}
