package br.com.hanrry.inventory.notification.service;

import br.com.hanrry.inventory.inventory.event.RestockQueueMessage;
import br.com.hanrry.inventory.notification.service.RestockNotificationStateService.ClaimOutcome;
import br.com.hanrry.inventory.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RestockNotificationProcessor {

    private static final String OWNER_NOT_FOUND_REASON = "Proprietário da mensagem de reposição não encontrado";

    private final RestockNotificationStateService restockNotificationStateService;
    private final UserRepository userRepository;
    private final StockAlertService stockAlertService;

    public void process(RestockQueueMessage message) {
        if (message.eventId() == null || message.productId() == null
                || message.occurredAt() == null || message.ownerId() == null) {
            log.error("Mensagem de reposição incompleta descartada. eventId={}", message.eventId());
            return;
        }

        ClaimOutcome claimOutcome = restockNotificationStateService.claim(message);
        if (claimOutcome != ClaimOutcome.PROCEED) {
            return;
        }

        var owner = userRepository.findById(message.ownerId());
        if (owner.isEmpty()) {
            restockNotificationStateService.markFailed(message.eventId(), OWNER_NOT_FOUND_REASON);
            log.error(
                    "Proprietário da mensagem de reposição não encontrado. eventId={}, ownerId={}",
                    message.eventId(),
                    message.ownerId()
            );
            return;
        }

        stockAlertService.checkInventoryAndNotify(owner.get());
        restockNotificationStateService.markSent(message.eventId());
        log.info("Notificação de reposição concluída. eventId={}", message.eventId());
    }
}
