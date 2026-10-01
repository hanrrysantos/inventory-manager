package br.com.hanrry.inventory.notification.event;

import br.com.hanrry.inventory.inventory.event.RestockQueueMessage;
import br.com.hanrry.inventory.notification.service.RestockNotificationProcessor;
import br.com.hanrry.inventory.shared.exception.notification.email.EmailSendException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RestockQueueListenerTest {

    private static final UUID EVENT_ID = UUID.fromString("5f1c2d3e-4a5b-6c7d-8e9f-0a1b2c3d4e5f");
    private static final Instant OCCURRED_AT = Instant.parse("2026-09-29T12:00:00Z");

    @Mock
    private RestockNotificationProcessor restockNotificationProcessor;

    @InjectMocks
    private RestockQueueListener listener;

    @Test
    void shouldListenOnlyOnRestockQueue() throws NoSuchMethodException {
        RabbitListener rabbitListener = RestockQueueListener.class
                .getMethod("on", RestockQueueMessage.class)
                .getAnnotation(RabbitListener.class);

        assertNotNull(rabbitListener);
        assertArrayEquals(new String[]{"inventory.restock-needed"}, rabbitListener.queues());
    }

    @Test
    void shouldDelegateToProcessor() {
        RestockQueueMessage message = new RestockQueueMessage(EVENT_ID, 13L, OCCURRED_AT, 7L);

        listener.on(message);

        verify(restockNotificationProcessor).process(message);
    }

    @Test
    void shouldPropagateProcessorFailureForRetry() {
        RestockQueueMessage message = new RestockQueueMessage(EVENT_ID, 13L, OCCURRED_AT, 7L);
        doThrow(new EmailSendException("Falha ao enviar alerta de estoque pelo Resend", null))
                .when(restockNotificationProcessor)
                .process(message);

        assertThrows(EmailSendException.class, () -> listener.on(message));
    }
}
