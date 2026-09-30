package br.com.hanrry.inventory.inventory.event;

import java.time.Instant;
import java.util.UUID;

public record RestockNeededEvent(UUID eventId, Long productId, Instant occurredAt) {
}
