package br.com.hanrry.inventory.controller;

import br.com.hanrry.inventory.controller.docs.AuthControllerDocs;
import br.com.hanrry.inventory.dto.auth.AuthRequestDTO;
import br.com.hanrry.inventory.dto.auth.AuthResponseDTO;
import br.com.hanrry.inventory.dto.user.UserRequestDTO;
import br.com.hanrry.inventory.dto.user.UserResponseDTO;
import br.com.hanrry.inventory.security.JwtUtil;
import br.com.hanrry.inventory.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "api/v1/auth")
public class AuthController implements AuthControllerDocs {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @PostMapping(value = "/register")
    public ResponseEntity<UserResponseDTO> registerUser(@RequestBody UserRequestDTO request){
        UserResponseDTO user = userService.createUser(request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(user.id()).toUri();

        return ResponseEntity.created(uri).body(user);
    }

    @PostMapping(value = "/login")
    public ResponseEntity<AuthResponseDTO> loginUser(@RequestBody AuthRequestDTO request){
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()
        ));

        String token = jwtUtil.generateToken(request.email());
        return ResponseEntity.ok().body(new AuthResponseDTO(token));
    }




}
