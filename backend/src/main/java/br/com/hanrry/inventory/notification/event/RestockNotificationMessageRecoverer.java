package br.com.hanrry.inventory.notification.event;

import br.com.hanrry.inventory.inventory.config.RestockQueueConfig;
import br.com.hanrry.inventory.inventory.event.RestockQueueMessage;
import br.com.hanrry.inventory.notification.entity.enums.RestockNotificationStatus;
import br.com.hanrry.inventory.notification.repository.RestockNotificationRepository;
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
        restockNotificationRepository.findByEventId(queueMessage.eventId()).ifPresent(notification -> {
            notification.setStatus(RestockNotificationStatus.FAILED);
            notification.setFailureReason(cause.getMessage());
            restockNotificationRepository.save(notification);
            log.error("Notificação de reposição marcada como FAILED. eventId={}", queueMessage.eventId(), cause);
        });
    }
}
