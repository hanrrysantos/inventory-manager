package br.com.hanrry.inventory.notification.service;

import br.com.hanrry.inventory.inventory.event.RestockQueueMessage;
import br.com.hanrry.inventory.notification.entity.RestockNotification;
import br.com.hanrry.inventory.notification.entity.enums.RestockNotificationStatus;
import br.com.hanrry.inventory.notification.repository.RestockNotificationRepository;
import br.com.hanrry.inventory.shared.observability.RestockMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RestockNotificationStateService {

    public enum ClaimOutcome {
        PROCEED,
        SKIP_ALREADY_DONE,
        SKIP_CONCURRENT
    }

    private final RestockNotificationRepository restockNotificationRepository;
    private final RestockMetrics restockMetrics;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ClaimOutcome claim(RestockQueueMessage message) {
        Optional<RestockNotification> existing = restockNotificationRepository.findByEventId(message.eventId());
        if (existing.isPresent()) {
            RestockNotificationStatus status = existing.get().getStatus();
            if (status == RestockNotificationStatus.SENT || status == RestockNotificationStatus.FAILED) {
                return ClaimOutcome.SKIP_ALREADY_DONE;
            }
            return ClaimOutcome.PROCEED;
        }

        RestockNotification notification = new RestockNotification();
        notification.setEventId(message.eventId());
        notification.setOwnerId(message.ownerId());
        notification.setProductId(message.productId());
        notification.setOccurredAt(message.occurredAt());
        notification.setStatus(RestockNotificationStatus.PENDING);

        try {
            restockNotificationRepository.save(notification);
            return ClaimOutcome.PROCEED;
        } catch (DataIntegrityViolationException exception) {
            RestockNotification concurrent = restockNotificationRepository.findByEventId(message.eventId())
                    .orElseThrow(() -> exception);
            if (concurrent.getStatus() == RestockNotificationStatus.PENDING) {
                return ClaimOutcome.SKIP_CONCURRENT;
            }
            return ClaimOutcome.SKIP_ALREADY_DONE;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSent(UUID eventId) {
        RestockNotification notification = restockNotificationRepository.findByEventId(eventId)
                .orElseThrow();
        RestockNotificationStatus previous = notification.getStatus();
        notification.setStatus(RestockNotificationStatus.SENT);
        notification.setFailureReason(null);
        restockNotificationRepository.save(notification);
        if (previous != RestockNotificationStatus.SENT) {
            restockMetrics.incrementSent();
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID eventId, String reason) {
        Optional<RestockNotification> existing = restockNotificationRepository.findByEventId(eventId);
        if (existing.isEmpty()) {
            restockMetrics.incrementFailed();
            return;
        }
        RestockNotification notification = existing.get();
        RestockNotificationStatus previous = notification.getStatus();
        notification.setStatus(RestockNotificationStatus.FAILED);
        notification.setFailureReason(reason);
        restockNotificationRepository.save(notification);
        if (previous != RestockNotificationStatus.FAILED) {
            restockMetrics.incrementFailed();
        }
    }
}
