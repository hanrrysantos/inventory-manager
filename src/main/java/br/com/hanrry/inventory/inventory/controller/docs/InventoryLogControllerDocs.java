package br.com.hanrry.inventory.inventory.controller.docs;

import br.com.hanrry.inventory.inventory.dto.invetoryLog.InventoryLogResponseDTO;
import br.com.hanrry.inventory.inventory.movement.LogType;
import br.com.hanrry.inventory.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

@Tag(name = "06 Movimentações", description = "Histórico paginado de entradas e saídas de estoque.")
public interface InventoryLogControllerDocs {

    @Operation(summary = "Lista movimentações paginadas",
            description = "Filtros opcionais: productId, batchId, type, from, to. Sort: timestamp, id.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Página retornada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetros de paginação ou sort inválidos")
    })
    ResponseEntity<PageResponse<InventoryLogResponseDTO>> findLogs(
            Long productId,
            Long batchId,
            LogType type,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Integer page,
            Integer size,
            @ParameterObject Pageable pageable
    );

    @Operation(summary = "Busca movimentação por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registro encontrado"),
            @ApiResponse(responseCode = "404", description = "Movimentação não encontrada")
    })
    ResponseEntity<InventoryLogResponseDTO> findLogById(Long id);
}
