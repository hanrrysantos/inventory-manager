package br.com.hanrry.inventory.notification.serviceTest;

import br.com.hanrry.inventory.notification.entity.RestockNotification;
import br.com.hanrry.inventory.notification.entity.enums.RestockNotificationStatus;
import br.com.hanrry.inventory.notification.repository.RestockNotificationRepository;
import br.com.hanrry.inventory.notification.service.RestockNotificationStateService;
import br.com.hanrry.inventory.shared.observability.RestockMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestockNotificationStateServiceTest {

    private static final UUID EVENT_ID = UUID.fromString("5f1c2d3e-4a5b-6c7d-8e9f-0a1b2c3d4e5f");

    @Mock
    private RestockNotificationRepository restockNotificationRepository;

    @Mock
    private RestockMetrics restockMetrics;

    @InjectMocks
    private RestockNotificationStateService restockNotificationStateService;

    @Test
    void shouldIncrementSentWhenStatusWasNotSent() {
        RestockNotification notification = pendingNotification();
        when(restockNotificationRepository.findByEventId(EVENT_ID)).thenReturn(Optional.of(notification));

        restockNotificationStateService.markSent(EVENT_ID);

        verify(restockMetrics).incrementSent();
    }

    @Test
    void shouldNotIncrementSentWhenAlreadySent() {
        RestockNotification notification = pendingNotification();
        notification.setStatus(RestockNotificationStatus.SENT);
        when(restockNotificationRepository.findByEventId(EVENT_ID)).thenReturn(Optional.of(notification));

        restockNotificationStateService.markSent(EVENT_ID);

        verify(restockMetrics, never()).incrementSent();
    }

    @Test
    void shouldIncrementFailedWhenStatusWasNotFailed() {
        RestockNotification notification = pendingNotification();
        when(restockNotificationRepository.findByEventId(EVENT_ID)).thenReturn(Optional.of(notification));

        restockNotificationStateService.markFailed(EVENT_ID, "boom");

        verify(restockMetrics).incrementFailed();
    }

    @Test
    void shouldNotIncrementFailedWhenAlreadyFailed() {
        RestockNotification notification = pendingNotification();
        notification.setStatus(RestockNotificationStatus.FAILED);
        when(restockNotificationRepository.findByEventId(EVENT_ID)).thenReturn(Optional.of(notification));

        restockNotificationStateService.markFailed(EVENT_ID, "boom");

        verify(restockMetrics, never()).incrementFailed();
    }

    @Test
    void shouldIncrementFailedWhenNotificationRowDoesNotExist() {
        when(restockNotificationRepository.findByEventId(EVENT_ID)).thenReturn(Optional.empty());

        restockNotificationStateService.markFailed(EVENT_ID, "owner ausente");

        verify(restockMetrics).incrementFailed();
    }

    private static RestockNotification pendingNotification() {
        RestockNotification notification = new RestockNotification();
        notification.setEventId(EVENT_ID);
        notification.setStatus(RestockNotificationStatus.PENDING);
        return notification;
    }
}
