package br.com.hanrry.inventory.notification.event;

import br.com.hanrry.inventory.inventory.event.RestockQueueMessage;
import br.com.hanrry.inventory.notification.service.RestockNotificationProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static br.com.hanrry.inventory.inventory.config.RestockQueueConfig.RESTOCK_NEEDED_QUEUE;

@Component
@RequiredArgsConstructor
public class RestockQueueListener {

    private final RestockNotificationProcessor restockNotificationProcessor;

    @RabbitListener(queues = RESTOCK_NEEDED_QUEUE)
    public void on(RestockQueueMessage message) {
        restockNotificationProcessor.process(message);
    }
}
