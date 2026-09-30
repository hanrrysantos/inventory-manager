package br.com.hanrry.inventory.notification.event;

import br.com.hanrry.inventory.inventory.event.RestockNeededEvent;
import br.com.hanrry.inventory.notification.service.StockAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class RestockNeededEventListener {

    private final StockAlertService stockAlertService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(RestockNeededEvent event) {
        try {
            stockAlertService.checkInventoryAndNotify();
        } catch (Exception exception) {
            log.error(
                    "Falha ao processar alerta de reposição após commit. eventId={}",
                    event.eventId(),
                    exception
            );
        }
    }
}
