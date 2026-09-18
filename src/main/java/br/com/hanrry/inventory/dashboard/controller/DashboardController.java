package br.com.hanrry.inventory.dashboard.controller;

import br.com.hanrry.inventory.dashboard.dto.DashboardSummaryDTO;
import br.com.hanrry.inventory.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Obtém o resumo do estoque para o dashboard")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumo retornado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Autenticação necessária"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDTO> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }
}
