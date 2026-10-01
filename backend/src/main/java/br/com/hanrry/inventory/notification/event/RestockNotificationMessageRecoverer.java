package br.com.hanrry.inventory.notification.event;

import br.com.hanrry.inventory.inventory.config.RestockQueueConfig;
import br.com.hanrry.inventory.inventory.event.RestockQueueMessage;
import br.com.hanrry.inventory.notification.repository.RestockNotificationRepository;
import br.com.hanrry.inventory.notification.service.RestockNotificationStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class RestockNotificationMessageRecoverer implements MessageRecoverer {

    private final RabbitTemplate rabbitTemplate;
    private final RestockNotificationRepository restockNotificationRepository;
    private final RestockNotificationStateService restockNotificationStateService;
    private final MessageConverter messageConverter;

    @Override
    public void recover(Message message, Throwable cause) {
        markFailedIfPossible(message, cause);
        MessageRecoverer delegate = new RepublishMessageRecoverer(
                rabbitTemplate,
                "",
                RestockQueueConfig.RESTOCK_NEEDED_DLQ
        );
        delegate.recover(message, cause);
        log.error("Mensagem de reposição enviada para DLQ após esgotar retry", cause);
    }

    private void markFailedIfPossible(Message message, Throwable cause) {
        Object body = messageConverter.fromMessage(message);
        if (!(body instanceof RestockQueueMessage queueMessage) || queueMessage.eventId() == null) {
            return;
        }
        if (restockNotificationRepository.findByEventId(queueMessage.eventId()).isEmpty()) {
            return;
        }
        restockNotificationStateService.markFailed(queueMessage.eventId(), cause.getMessage());
        log.error("Notificação de reposição marcada como FAILED. eventId={}", queueMessage.eventId(), cause);
    }
}
