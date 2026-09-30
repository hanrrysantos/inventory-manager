package br.com.hanrry.inventory.notification.serviceTest;

import br.com.hanrry.inventory.inventory.event.RestockQueueMessage;
import br.com.hanrry.inventory.notification.service.RestockNotificationProcessor;
import br.com.hanrry.inventory.notification.service.RestockNotificationStateService;
import br.com.hanrry.inventory.notification.service.StockAlertService;
import br.com.hanrry.inventory.shared.exception.notification.email.EmailSendException;
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
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static br.com.hanrry.inventory.notification.service.RestockNotificationStateService.ClaimOutcome;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class RestockNotificationProcessorTest {

    private static final UUID EVENT_ID = UUID.fromString("5f1c2d3e-4a5b-6c7d-8e9f-0a1b2c3d4e5f");
    private static final Instant OCCURRED_AT = Instant.parse("2026-09-29T12:00:00Z");

    @Mock
    private RestockNotificationStateService restockNotificationStateService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StockAlertService stockAlertService;

    @InjectMocks
    private RestockNotificationProcessor processor;

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
    void shouldDiscardIncompleteMessageWithoutClaiming(RestockQueueMessage message, CapturedOutput output) {
        assertDoesNotThrow(() -> processor.process(message));

        verifyNoInteractions(restockNotificationStateService, stockAlertService);
        assertThat(output.getAll()).contains("ERROR");
    }

    @Test
    void shouldSkipWhenClaimReturnsAlreadyDone() {
        RestockQueueMessage message = new RestockQueueMessage(EVENT_ID, 13L, OCCURRED_AT, 7L);
        when(restockNotificationStateService.claim(message)).thenReturn(ClaimOutcome.SKIP_ALREADY_DONE);

        processor.process(message);

        verify(restockNotificationStateService, never()).markSent(any());
        verifyNoInteractions(stockAlertService);
    }

    @Test
    void shouldSkipWhenClaimReturnsConcurrent() {
        RestockQueueMessage message = new RestockQueueMessage(EVENT_ID, 13L, OCCURRED_AT, 7L);
        when(restockNotificationStateService.claim(message)).thenReturn(ClaimOutcome.SKIP_CONCURRENT);

        processor.process(message);

        verifyNoInteractions(stockAlertService);
    }

    @Test
    void shouldMarkFailedWhenOwnerDoesNotExist() {
        RestockQueueMessage message = new RestockQueueMessage(EVENT_ID, 13L, OCCURRED_AT, 7L);
        when(restockNotificationStateService.claim(message)).thenReturn(ClaimOutcome.PROCEED);
        when(userRepository.findById(7L)).thenReturn(Optional.empty());

        processor.process(message);

        verify(restockNotificationStateService).markFailed(EVENT_ID, "Proprietário da mensagem de reposição não encontrado");
        verifyNoInteractions(stockAlertService);
    }

    @Test
    void shouldNotifyOwnerAndMarkSentWhenClaimProceeds() {
        RestockQueueMessage message = new RestockQueueMessage(EVENT_ID, 13L, OCCURRED_AT, 7L);
        User owner = new User();
        owner.setId(7L);
        when(restockNotificationStateService.claim(message)).thenReturn(ClaimOutcome.PROCEED);
        when(userRepository.findById(7L)).thenReturn(Optional.of(owner));

        processor.process(message);

        verify(stockAlertService).checkInventoryAndNotify(owner);
        verify(restockNotificationStateService).markSent(EVENT_ID);
    }

    @Test
    void shouldPropagateEmailFailureForRetry() {
        RestockQueueMessage message = new RestockQueueMessage(EVENT_ID, 13L, OCCURRED_AT, 7L);
        User owner = new User();
        owner.setId(7L);
        when(restockNotificationStateService.claim(message)).thenReturn(ClaimOutcome.PROCEED);
        when(userRepository.findById(7L)).thenReturn(Optional.of(owner));
        doThrow(new EmailSendException("Falha ao enviar alerta de estoque pelo Resend", null))
                .when(stockAlertService)
                .checkInventoryAndNotify(owner);

        assertThrows(EmailSendException.class, () -> processor.process(message));

        verify(restockNotificationStateService, never()).markSent(eq(EVENT_ID));
    }
}
