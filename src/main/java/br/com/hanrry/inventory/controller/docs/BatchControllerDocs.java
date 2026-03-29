package br.com.hanrry.inventory.controller.docs;

import br.com.hanrry.inventory.dto.batch.AddStockBatchRequestDTO;
import br.com.hanrry.inventory.dto.batch.BatchRequestDTO;
import br.com.hanrry.inventory.dto.batch.BatchResponseDTO;
import br.com.hanrry.inventory.dto.batch.ConsumeBatchRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "05 Lotes", description = "Endpoints para criação de lotes, adição e remoção de produtos de um lote e lista dos lotes estragados.")
public interface BatchControllerDocs {

    @Operation(
            summary = "Cria um novo lote",
            description = "Cadastra um novo lote vinculado a um produto específico. "
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Lote criado e registrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou número de lote já existente"),
            @ApiResponse(responseCode = "404", description = "Produto informado não encontrado"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão - Requer Role ADMIN")
    })
    ResponseEntity<BatchResponseDTO> createBatch(BatchRequestDTO request);

    @Operation(
            summary = "Adiciona quantidade a um lote existente",
            description = "Incrementa o saldo de um lote específico."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estoque do lote atualizado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Lote não encontrado"),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão - Requer Role USER ou ADMIN")
    })
    ResponseEntity<BatchResponseDTO> addStock(Long id, AddStockBatchRequestDTO request);

    @Operation(
            summary = "Consumo inteligente de estoque ",
            description = "Realiza a saída de produtos do estoque global de um produto. "
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Saída realizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Quantidade solicitada maior do que o estoque disponível"),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado")
    })
    ResponseEntity<Void> consume(ConsumeBatchRequestDTO request);

    @Operation(
            summary = "Lista lotes com validade vencida",
            description = "Retorna todos os lotes cadastrados cuja data de expiração é menor do que a data atual do servidor."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de lotes vencidos retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    })
    ResponseEntity<List<BatchResponseDTO>> listExpired();
}
