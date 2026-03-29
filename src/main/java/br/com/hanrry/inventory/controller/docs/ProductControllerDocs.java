package br.com.hanrry.inventory.controller.docs;

import br.com.hanrry.inventory.dto.product.ProductRequestDTO;
import br.com.hanrry.inventory.dto.product.ProductResponseDTO;
import br.com.hanrry.inventory.dto.product.UpdateProdcutRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "04 Produtos", description = "Endpoints para gerenciar produtos e monitorar níveis de estoque.")
public interface ProductControllerDocs {

    @Operation(summary = "Cria um novo produto",
            description = "Cria um produto respectivamente a uma categoria existente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Produto criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Erro de validação nos campos ou nome duplicado"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado - Requer Role ADMIN"),
            @ApiResponse(responseCode = "404", description = "Categoria informada não encontrada")
    })
    ResponseEntity<ProductResponseDTO> createProduct(ProductRequestDTO request);

    @Operation(summary = "Busca produto por ID",
            description = "Busca um produto específico pelo seu ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Produto retornado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado - Requer Role ADMIN"),
            @ApiResponse(responseCode = "404", description = "ID do produto não localizado no banco")
    })
    ResponseEntity<ProductResponseDTO> findProductById(Long id);

    @Operation(summary = "Lista todos os produtos",
            description = "Retorna o catálogo completo disponível.")
            @ApiResponse(responseCode = "200", description = "Operação realizada com sucesso")
    ResponseEntity<List<ProductResponseDTO>> findAllProducts();

    @Operation(summary = "Lista produtos abaixo do estoque mínimo",
            description = "Filtra produtos onde a soma de todos os lotes é menor do que o estoque mínimo.")
            @ApiResponse(responseCode = "200", description = "Lista de alertas gerada")
    ResponseEntity<List<ProductResponseDTO>> getLowStock();

    @Operation(summary = "Atualiza dados cadastrais",
            description = "Altera nome, preço ou estoque de segurança.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Produto atualizado"),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado - Requer Role ADMIN")
    })
    ResponseEntity<ProductResponseDTO> updateProduct(Long id, UpdateProdcutRequestDTO request);

    @Operation(summary = "Exclui um produto",
            description = "Exclusão de um produto a partir do seu ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Produto deletado com sucesso"),
            @ApiResponse(responseCode = "404", description = "ID do produto não localizado no banco"),
            @ApiResponse(responseCode = "409", description = "Produto possui lotes e não pode ser removido")
    })
    ResponseEntity<Void> deleteProduct(Long id);
}