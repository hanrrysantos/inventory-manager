package br.com.hanrry.inventory.notification.event;

import br.com.hanrry.inventory.inventory.event.RestockNeededEvent;
import br.com.hanrry.inventory.notification.service.StockAlertService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RestockNeededEventListenerTest {

    @Mock
    private StockAlertService stockAlertService;

    @InjectMocks
    private RestockNeededEventListener listener;

    @Test
    void shouldDelegateToStockAlertService() {
        RestockNeededEvent event = new RestockNeededEvent(UUID.randomUUID(), 1L, Instant.now());

        listener.on(event);

        verify(stockAlertService).checkInventoryAndNotify();
    }

    @Test
    void shouldSwallowExceptionFromStockAlertService() {
        RestockNeededEvent event = new RestockNeededEvent(UUID.randomUUID(), 1L, Instant.now());
        doThrow(new RuntimeException("Falha no envio"))
                .when(stockAlertService)
                .checkInventoryAndNotify();

        assertDoesNotThrow(() -> listener.on(event));

        verify(stockAlertService).checkInventoryAndNotify();
    }
}
