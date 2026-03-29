package br.com.hanrry.inventory.controller.docs;

import br.com.hanrry.inventory.dto.auth.AuthRequestDTO;
import br.com.hanrry.inventory.dto.auth.AuthResponseDTO;
import br.com.hanrry.inventory.dto.user.UserRequestDTO;
import br.com.hanrry.inventory.dto.user.UserResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "01 Autenticação", description = "Endpoints públicos para login e registo de novos usuários")
public interface AuthControllerDocs {

    @Operation(summary = "Autentica um usuário",
            description = "Verifica as credenciais do usuário e retorna um token JWT para acesso às rotas protegidas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login realizado com sucesso (Token JWT retornado)"),
            @ApiResponse(responseCode = "400", description = "Erro de validação - Email ou senha em formato inválidos"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas (E-mail ou senha incorretos)")
    })
    ResponseEntity<AuthResponseDTO> loginUser(AuthRequestDTO request);

    @Operation(summary = "Regista um novo usuário",
            description = "Cria uma nova conta de usuário no sistema com a permissão padrão de 'USER'.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuário registado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Erro de validação nos dados enviados"),
            @ApiResponse(responseCode = "409", description = "Já existe um usuário registado com este e-mail")
    })
    ResponseEntity<UserResponseDTO> registerUser(UserRequestDTO dto);
}
