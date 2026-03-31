package br.com.hanrry.inventory.controller.docs;

import br.com.hanrry.inventory.dto.user.UpdateUserRequestDTO;
import br.com.hanrry.inventory.dto.user.UserResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "02 Usuários", description = "Gestão de acessos e perfis - Apenas para ADMIN")
public interface UserControllerDocs {

    @Operation(summary = "Busca um usuário pelo ID",
            description = "Busca apenas um usuário específico, pelo seu ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário localizado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado - Requer Role ADMIN")
    })
    ResponseEntity<UserResponseDTO> findUserById(Long id);

    @Operation(summary = "Lista todos os usuários",
            description = "Retorna uma lista com todos usuários")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado - Requer Role ADMIN")
    })
    ResponseEntity<List<UserResponseDTO>> findAllUsers();

    @Operation(summary = "Atualiza perfil de usuário",
            description = "Permite alterar a senha.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "E-mail já está em uso por outro usuário"),
            @ApiResponse(responseCode = "404", description = "ID inexistente"),
            @ApiResponse(responseCode = "403", description = "Acesso negado - Requer Role ADMIN")
    })
    ResponseEntity<UserResponseDTO> updateUser(Long id, UpdateUserRequestDTO request);

    @Operation(summary = "Exclui um usuário",
            description = "Remove o acesso de um colaborador do sistema.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Acesso removido com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado - Requer Role ADMIN")
    })
    ResponseEntity<Void> deleteUserById(Long id);
}