package br.com.hanrry.inventory.notification.event;

import br.com.hanrry.inventory.inventory.event.RestockQueueMessage;
import br.com.hanrry.inventory.notification.service.StockAlertService;
import br.com.hanrry.inventory.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static br.com.hanrry.inventory.inventory.config.RestockQueueConfig.RESTOCK_NEEDED_QUEUE;

@Component
@Slf4j
@RequiredArgsConstructor
public class RestockQueueListener {

    private final StockAlertService stockAlertService;
    private final UserRepository userRepository;

    @RabbitListener(queues = RESTOCK_NEEDED_QUEUE)
    public void on(RestockQueueMessage message) {
        if (message.eventId() == null || message.productId() == null
                || message.occurredAt() == null || message.ownerId() == null) {
            log.error("Mensagem de reposição incompleta descartada. eventId={}", message.eventId());
            return;
        }

        try {
            userRepository.findById(message.ownerId()).ifPresentOrElse(
                    stockAlertService::checkInventoryAndNotify,
                    () -> log.error(
                            "Proprietário da mensagem de reposição não encontrado. eventId={}, ownerId={}",
                            message.eventId(),
                            message.ownerId()
                    )
            );
        } catch (Exception exception) {
            log.error(
                    "Falha ao processar alerta de reposição da fila. eventId={}",
                    message.eventId(),
                    exception
            );
        }
    }
}
