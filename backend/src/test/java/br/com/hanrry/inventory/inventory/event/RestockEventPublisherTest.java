package br.com.hanrry.inventory.inventory.event;

import br.com.hanrry.inventory.shared.exception.security.OwnerNotAuthenticatedException;
import br.com.hanrry.inventory.shared.observability.RestockMetrics;
import br.com.hanrry.inventory.shared.security.OwnerContext;
import br.com.hanrry.inventory.shared.web.RequestIdFilter;
import br.com.hanrry.inventory.user.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.net.ConnectException;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class RestockEventPublisherTest {

    private static final String RESTOCK_QUEUE = "inventory.restock-needed";
    private static final String REQUEST_ID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private OwnerContext ownerContext;

    @Mock
    private RestockMetrics restockMetrics;

    @InjectMocks
    private RestockEventPublisher publisher;

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void shouldPublishOnlyAfterCommit() throws NoSuchMethodException {
        TransactionalEventListener listener = RestockEventPublisher.class
                .getMethod("on", RestockNeededEvent.class)
                .getAnnotation(TransactionalEventListener.class);

        assertNotNull(listener);
        assertEquals(TransactionPhase.AFTER_COMMIT, listener.phase());
    }

    @Test
    void shouldSendMessageWithEventFieldsAndAuthenticatedOwnerToRestockQueue(CapturedOutput output) {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-09-29T12:00:00Z");
        RestockNeededEvent event = new RestockNeededEvent(eventId, 13L, occurredAt);
        User owner = new User();
        owner.setId(7L);
        when(ownerContext.currentUser()).thenReturn(owner);
        MDC.put(RequestIdFilter.REQUEST_ID_MDC_KEY, REQUEST_ID);

        publisher.on(event);

        ArgumentCaptor<RestockQueueMessage> message = ArgumentCaptor.forClass(RestockQueueMessage.class);
        verify(rabbitTemplate).convertAndSend(eq(RESTOCK_QUEUE), message.capture());
        assertEquals(new RestockQueueMessage(eventId, 13L, occurredAt, 7L), message.getValue());
        verify(restockMetrics).incrementConsumption();
        verify(restockMetrics).incrementPublished();
        assertThat(output.getAll()).contains("eventId=" + eventId);
        assertThat(output.getAll()).contains("requestId=" + REQUEST_ID);
    }

    @Test
    void shouldLogEventIdAndNotRethrowWhenBrokerFails(CapturedOutput output) {
        UUID eventId = UUID.randomUUID();
        RestockNeededEvent event = new RestockNeededEvent(eventId, 13L, Instant.now());
        User owner = new User();
        owner.setId(7L);
        when(ownerContext.currentUser()).thenReturn(owner);
        doThrow(new AmqpConnectException(new ConnectException("Connection refused")))
                .when(rabbitTemplate)
                .convertAndSend(anyString(), any(Object.class));
        MDC.put(RequestIdFilter.REQUEST_ID_MDC_KEY, REQUEST_ID);

        assertDoesNotThrow(() -> publisher.on(event));

        assertThat(output.getAll()).contains("eventId=" + eventId);
        assertThat(output.getAll()).contains("requestId=" + REQUEST_ID);
        verify(restockMetrics).incrementConsumption();
        verify(restockMetrics, never()).incrementPublished();
    }

    @Test
    void shouldNotSendAndShouldLogEventIdWhenOwnerIsNotAuthenticated(CapturedOutput output) {
        UUID eventId = UUID.randomUUID();
        RestockNeededEvent event = new RestockNeededEvent(eventId, 13L, Instant.now());
        when(ownerContext.currentUser())
                .thenThrow(new OwnerNotAuthenticatedException("Authenticated owner is required"));

        assertDoesNotThrow(() -> publisher.on(event));

        verifyNoInteractions(rabbitTemplate);
        assertThat(output.getAll()).contains("eventId=" + eventId);
        verify(restockMetrics).incrementConsumption();
        verify(restockMetrics, never()).incrementPublished();
    }
}
