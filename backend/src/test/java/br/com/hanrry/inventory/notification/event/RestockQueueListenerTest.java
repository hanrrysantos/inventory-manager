package br.com.hanrry.inventory.notification.event;

import br.com.hanrry.inventory.inventory.event.RestockQueueMessage;
import br.com.hanrry.inventory.notification.service.StockAlertService;
import br.com.hanrry.inventory.shared.exception.notification.email.EmailSendException;
import br.com.hanrry.inventory.shared.exception.notification.pdf.WritePdfException;
import br.com.hanrry.inventory.user.entity.User;
import br.com.hanrry.inventory.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class RestockQueueListenerTest {

    private static final UUID EVENT_ID = UUID.fromString("5f1c2d3e-4a5b-6c7d-8e9f-0a1b2c3d4e5f");
    private static final Instant OCCURRED_AT = Instant.parse("2026-09-29T12:00:00Z");

    @Mock
    private StockAlertService stockAlertService;

    @Mock
    private UserRepository userRepository;

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
    void shouldNotifyLowStockOfMessageOwner() {
        User owner = new User();
        owner.setId(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(owner));

        listener.on(new RestockQueueMessage(EVENT_ID, 13L, OCCURRED_AT, 7L));

        verify(stockAlertService).checkInventoryAndNotify(owner);
    }

    static Stream<Arguments> incompleteMessages() {
        return Stream.of(
                Arguments.of(new RestockQueueMessage(null, 13L, OCCURRED_AT, 7L)),
                Arguments.of(new RestockQueueMessage(EVENT_ID, null, OCCURRED_AT, 7L)),
                Arguments.of(new RestockQueueMessage(EVENT_ID, 13L, null, 7L)),
                Arguments.of(new RestockQueueMessage(EVENT_ID, 13L, OCCURRED_AT, null))
        );
    }

    @ParameterizedTest
    @MethodSource("incompleteMessages")
    void shouldLogAndNotNotifyWhenMessageIsIncomplete(RestockQueueMessage message, CapturedOutput output) {
        User owner = new User();
        owner.setId(7L);
        lenient().when(userRepository.findById(any())).thenReturn(Optional.of(owner));

        assertDoesNotThrow(() -> listener.on(message));

        verifyNoInteractions(stockAlertService);
        assertThat(output.getAll()).contains("ERROR");
        if (message.eventId() != null) {
            assertThat(output.getAll()).contains("eventId=" + message.eventId());
        }
    }

    @Test
    void shouldLogAndNotNotifyWhenOwnerDoesNotExist(CapturedOutput output) {
        when(userRepository.findById(7L)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> listener.on(new RestockQueueMessage(EVENT_ID, 13L, OCCURRED_AT, 7L)));

        verifyNoInteractions(stockAlertService);
        assertThat(output.getAll()).contains("eventId=" + EVENT_ID);
    }

    @Test
    void shouldLogEventIdAndNotRethrowWhenPdfGenerationFails(CapturedOutput output) {
        User owner = new User();
        owner.setId(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(owner));
        doThrow(new WritePdfException("Error generating PDF"))
                .when(stockAlertService)
                .checkInventoryAndNotify(owner);

        assertDoesNotThrow(() -> listener.on(new RestockQueueMessage(EVENT_ID, 13L, OCCURRED_AT, 7L)));

        assertThat(output.getAll()).contains("eventId=" + EVENT_ID);
    }

    @Test
    void shouldLogEventIdAndNotRethrowWhenResendFails(CapturedOutput output) {
        User owner = new User();
        owner.setId(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(owner));
        doThrow(new EmailSendException("Falha ao enviar alerta de estoque pelo Resend", null))
                .when(stockAlertService)
                .checkInventoryAndNotify(owner);

        assertDoesNotThrow(() -> listener.on(new RestockQueueMessage(EVENT_ID, 13L, OCCURRED_AT, 7L)));

        assertThat(output.getAll()).contains("eventId=" + EVENT_ID);
    }
}
