package br.com.hanrry.inventory.product.controller.docs;

import br.com.hanrry.inventory.product.dto.category.CategoryRequestDTO;
import br.com.hanrry.inventory.product.dto.category.CategoryResponseDTO;
import br.com.hanrry.inventory.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "03 Categorias", description = "Endpoints para gerenciar diferentes categorias de produtos.")
public interface CategoryControllerDocs {

    @Operation(
            summary = "Cria uma nova categoria",
            description = "Cadastra uma nova categoria no sistema para organizar os produtos."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Categoria criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou categoria já existente"),
            @ApiResponse(responseCode = "403", description = "Acesso negado - Requer Role ADMIN")
    })
    ResponseEntity<CategoryResponseDTO> createCategory(CategoryRequestDTO request);

    @Operation(
            summary = "Busca categoria por ID",
            description = "Retorna os detalhes de uma categoria específica."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Categoria encontrada"),
            @ApiResponse(responseCode = "404", description = "Categoria não encontrada")
    })
    ResponseEntity<CategoryResponseDTO> findCategoryById(Long id);

    @Operation(
            summary = "Lista categorias paginadas",
            description = "Retorna categorias paginadas. Parâmetros: page (0-based), size (máx. 100), sort (id, name)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetros de paginação ou sort inválidos")
    })
    ResponseEntity<PageResponse<CategoryResponseDTO>> findAllCategories(
            Integer page,
            Integer size,
            @ParameterObject Pageable pageable
    );

    @Operation(
            summary = "Atualiza uma categoria",
            description = "Permite alterar o nome ou outras informações de uma categoria existente."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Categoria atualizada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Categoria não encontrada para o ID informado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado - Requer Role ADMIN")
    })
    ResponseEntity<CategoryResponseDTO> updateCategory(Long id, CategoryRequestDTO request);

    @Operation(
            summary = "Exclui uma categoria",
            description = "Remove uma categoria do sistema."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Categoria removida com sucesso"),
            @ApiResponse(responseCode = "404", description = "Categoria não encontrada"),
            @ApiResponse(responseCode = "403", description = "Acesso negado - Requer Role ADMIN")
    })
    ResponseEntity<Void> deleteCategory(Long id);
}
