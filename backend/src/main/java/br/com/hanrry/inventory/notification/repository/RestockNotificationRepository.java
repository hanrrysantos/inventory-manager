package br.com.hanrry.inventory.notification.repository;

import br.com.hanrry.inventory.notification.entity.RestockNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RestockNotificationRepository extends JpaRepository<RestockNotification, Long> {

    Optional<RestockNotification> findByEventId(UUID eventId);
}
