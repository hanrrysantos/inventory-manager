package br.com.hanrry.inventory.inventory.event;

import br.com.hanrry.inventory.shared.security.OwnerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static br.com.hanrry.inventory.inventory.config.RestockQueueConfig.RESTOCK_NEEDED_QUEUE;

@Component
@Slf4j
@RequiredArgsConstructor
public class RestockEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final OwnerContext ownerContext;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(RestockNeededEvent event) {
        try {
            var owner = ownerContext.currentUser();
            rabbitTemplate.convertAndSend(RESTOCK_NEEDED_QUEUE, new RestockQueueMessage(
                    event.eventId(),
                    event.productId(),
                    event.occurredAt(),
                    owner.getId()
            ));
        } catch (Exception exception) {
            log.error(
                    "Falha ao publicar reposição na fila após commit. eventId={}",
                    event.eventId(),
                    exception
            );
        }
    }
}
