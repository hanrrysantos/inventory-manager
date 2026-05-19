package br.com.hanrry.inventory.controllerTest;

import br.com.hanrry.inventory.controller.BatchController;
import br.com.hanrry.inventory.dto.batch.AddStockBatchRequestDTO;
import br.com.hanrry.inventory.dto.batch.BatchRequestDTO;
import br.com.hanrry.inventory.dto.batch.BatchResponseDTO;
import br.com.hanrry.inventory.dto.batch.ConsumeBatchRequestDTO;
import br.com.hanrry.inventory.service.BatchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BatchControllerTest {

    @Mock
    private BatchService batchService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        BatchController batchController = new BatchController(batchService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(batchController)
                .build();
    }

    @Test
    void shouldListExpiredBatches() throws Exception {
        BatchResponseDTO batch = new BatchResponseDTO(
                1L,
                "BATCH-001",
                10L,
                LocalDate.of(2025, 1, 10),
                LocalDate.of(2026, 1, 10),
                BigDecimal.valueOf(2500),
                1L,
                "Notebook"
        );

        when(batchService.findExpiredBatches())
                .thenReturn(List.of(batch));

        mockMvc.perform(get("/api/v1/batches/expired"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].batchNumber").value("BATCH-001"))
                .andExpect(jsonPath("$[0].quantity").value(10L))
                .andExpect(jsonPath("$[0].productId").value(1L))
                .andExpect(jsonPath("$[0].productName").value("Notebook"));

        verify(batchService).findExpiredBatches();
    }

    @Test
    void shouldCreateBatch() throws Exception {
        BatchRequestDTO request = new BatchRequestDTO(
                "BATCH-001",
                10L,
                LocalDate.of(2025, 1, 10),
                LocalDate.of(2026, 1, 10),
                BigDecimal.valueOf(2500),
                1L
        );

        BatchResponseDTO response = new BatchResponseDTO(
                1L,
                "BATCH-001",
                10L,
                LocalDate.of(2025, 1, 10),
                LocalDate.of(2026, 1, 10),
                BigDecimal.valueOf(2500),
                1L,
                "Notebook"
        );

        when(batchService.createBatch(request))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/batches")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/batches/1"))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.batchNumber").value("BATCH-001"))
                .andExpect(jsonPath("$.quantity").value(10L))
                .andExpect(jsonPath("$.productName").value("Notebook"));

        verify(batchService).createBatch(request);
    }

    @Test
    void shouldAddStock() throws Exception {
        AddStockBatchRequestDTO request = new AddStockBatchRequestDTO(
                5L
        );

        BatchResponseDTO response = new BatchResponseDTO(
                1L,
                "BATCH-001",
                15L,
                LocalDate.of(2025, 1, 10),
                LocalDate.of(2026, 1, 10),
                BigDecimal.valueOf(2500),
                1L,
                "Notebook"
        );

        when(batchService.addStock(1L, request))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/batches/1/add")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.quantity").value(15L))
                .andExpect(jsonPath("$.productName").value("Notebook"));

        verify(batchService).addStock(1L, request);
    }

    @Test
    void shouldConsumeStock() throws Exception {
        ConsumeBatchRequestDTO request = new ConsumeBatchRequestDTO(
                1L,
                5L
        );

        doNothing().when(batchService).consumeStock(request);

        mockMvc.perform(post("/api/v1/batches/consume")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(batchService).consumeStock(request);
    }
}