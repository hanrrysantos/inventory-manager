package br.com.hanrry.inventory.inventory.controllerTest;

import br.com.hanrry.inventory.inventory.controller.InventoryLogController;
import br.com.hanrry.inventory.inventory.dto.invetoryLog.InventoryLogResponseDTO;
import br.com.hanrry.inventory.inventory.movement.LogType;
import br.com.hanrry.inventory.inventory.service.InventoryLogService;
import br.com.hanrry.inventory.shared.dto.PageResponse;
import br.com.hanrry.inventory.shared.exception.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InventoryLogControllerTest {

    @Mock
    private InventoryLogService inventoryLogService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new InventoryLogController(inventoryLogService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void shouldFindLogsPaginated() throws Exception {
        InventoryLogResponseDTO log = new InventoryLogResponseDTO(
                1L,
                LogType.INPUT,
                LocalDateTime.of(2026, 1, 10, 10, 0),
                5L,
                2L,
                10L,
                "Notebook",
                "LOT-001"
        );

        PageResponse<InventoryLogResponseDTO> page = new PageResponse<>(
                List.of(log),
                0,
                20,
                1,
                1
        );

        when(inventoryLogService.findLogs(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/inventory-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("INPUT"))
                .andExpect(jsonPath("$.content[0].productId").value(2))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(inventoryLogService).findLogs(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void shouldFindLogById() throws Exception {
        InventoryLogResponseDTO log = new InventoryLogResponseDTO(
                1L,
                LogType.OUTPUT,
                LocalDateTime.of(2026, 1, 10, 10, 0),
                3L,
                2L,
                10L,
                "Notebook",
                "LOT-001"
        );

        when(inventoryLogService.findLogById(1L)).thenReturn(log);

        mockMvc.perform(get("/api/v1/inventory-logs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.batchNumber").value("LOT-001"));
    }
}
