package br.com.hanrry.inventory.inventory.serviceTest;

import br.com.hanrry.inventory.inventory.batch.Batch;
import br.com.hanrry.inventory.inventory.movement.InventoryLog;
import br.com.hanrry.inventory.product.entity.Product;
import br.com.hanrry.inventory.inventory.movement.LogType;
import br.com.hanrry.inventory.inventory.dto.invetoryLog.InventoryLogResponseDTO;
import br.com.hanrry.inventory.inventory.mapper.InventoryLogMapper;
import br.com.hanrry.inventory.inventory.repository.InventoryLogRepository;
import br.com.hanrry.inventory.inventory.service.InventoryLogService;
import br.com.hanrry.inventory.shared.dto.PageResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryLogServiceTest {

    @Mock
    private InventoryLogRepository logRepository;

    @Mock
    private InventoryLogMapper inventoryLogMapper;

    @Mock
    private br.com.hanrry.inventory.shared.security.OwnerContext ownerContext;

    @InjectMocks
    private InventoryLogService inventoryLogService;

    @Test
    void shouldCreateInventoryLogSuccessfully() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Notebook");

        Batch batch = new Batch();
        batch.setId(1L);
        batch.setProduct(product);

        Long quantity = 10L;
        LogType type = LogType.INPUT;

        inventoryLogService.createLog(batch, quantity, type);

        ArgumentCaptor<InventoryLog> logCaptor =
                ArgumentCaptor.forClass(InventoryLog.class);

        verify(logRepository).save(logCaptor.capture());

        InventoryLog savedLog = logCaptor.getValue();

        assertEquals(batch, savedLog.getBatch());
        assertEquals(product, savedLog.getProduct());
        assertEquals(quantity, savedLog.getQuantity());
        assertEquals(type, savedLog.getType());
    }

    @Test
    void shouldFindLogsPaginated() {
        InventoryLog log = new InventoryLog();
        log.setId(1L);
        InventoryLogResponseDTO dto = new InventoryLogResponseDTO(
                1L, LogType.INPUT, null, 5L, 1L, 10L, "Notebook", "LOT-1"
        );
        var pageable = PageRequest.of(0, 20);
        Page<InventoryLog> page = new PageImpl<>(List.of(log), pageable, 1);

        when(logRepository.findFiltered(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(pageable)))
                .thenReturn(page);
        when(inventoryLogMapper.toDTO(log)).thenReturn(dto);

        PageResponse<InventoryLogResponseDTO> result =
                inventoryLogService.findLogs(null, null, null, null, null, pageable);

        assertEquals(1, result.content().size());
    }
}
