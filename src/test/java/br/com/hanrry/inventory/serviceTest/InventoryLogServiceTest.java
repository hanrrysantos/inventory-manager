package br.com.hanrry.inventory.serviceTest;

import br.com.hanrry.inventory.entity.Batch;
import br.com.hanrry.inventory.entity.InventoryLog;
import br.com.hanrry.inventory.entity.Product;
import br.com.hanrry.inventory.entity.enums.LogType;
import br.com.hanrry.inventory.repository.InventoryLogRepository;
import br.com.hanrry.inventory.service.InventoryLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InventoryLogServiceTest {

    @Mock
    private InventoryLogRepository logRepository;

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
}