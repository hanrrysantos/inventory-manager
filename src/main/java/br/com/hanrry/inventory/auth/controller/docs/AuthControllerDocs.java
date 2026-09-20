package br.com.hanrry.inventory.auth.controller.docs;

import br.com.hanrry.inventory.auth.dto.AuthRequestDTO;
import br.com.hanrry.inventory.auth.dto.AuthResponseDTO;
import br.com.hanrry.inventory.auth.dto.GoogleAuthRequestDTO;
import br.com.hanrry.inventory.user.dto.UserRequestDTO;
import br.com.hanrry.inventory.user.dto.UserResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.security.Principal;

@Tag(name = "01 Autenticação", description = "Endpoints de login, registo e vínculo de conta Google")
public interface AuthControllerDocs {

    @Operation(summary = "Autentica um usuário",
            description = "Verifica as credenciais do usuário e retorna um token JWT para acesso às rotas protegidas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login realizado com sucesso (Token JWT retornado)"),
            @ApiResponse(responseCode = "400", description = "Erro de validação - Email ou senha em formato inválidos"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas (E-mail ou senha incorretos)")
    })
    ResponseEntity<AuthResponseDTO> loginUser(AuthRequestDTO request);

    @Operation(summary = "Autentica com Google",
            description = "Valida o ID token Google e retorna um JWT da aplicação para uma conta Google vinculada.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login realizado com sucesso"),
            @ApiResponse(responseCode = "401", description = "ID token inválido, expirado ou com claims obrigatórias inválidas"),
            @ApiResponse(responseCode = "409", description = "A conta local precisa ser vinculada ao Google em uma sessão autenticada")
    })
    ResponseEntity<AuthResponseDTO> loginWithGoogle(GoogleAuthRequestDTO request);

    @Operation(summary = "Vincula uma conta Google",
            description = "Vincula o ID Google da credencial apresentada ao usuário autenticado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Conta Google vinculada"),
            @ApiResponse(responseCode = "401", description = "JWT ou ID token inválido"),
            @ApiResponse(responseCode = "409", description = "A conta Google já pertence a outro usuário")
    })
    ResponseEntity<Void> linkGoogleAccount(GoogleAuthRequestDTO request, Principal principal);

    @Operation(summary = "Regista um novo usuário",
            description = "Cria uma nova conta de usuário no sistema com a permissão padrão de 'USER'.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuário registado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Erro de validação nos dados enviados"),
            @ApiResponse(responseCode = "409", description = "Já existe um usuário registado com este e-mail")
    })
    ResponseEntity<UserResponseDTO> registerUser(UserRequestDTO dto);
}
