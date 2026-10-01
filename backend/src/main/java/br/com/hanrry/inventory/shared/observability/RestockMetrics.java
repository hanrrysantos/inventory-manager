package br.com.hanrry.inventory.shared.observability;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class RestockMetrics {

    static final String CONSUMPTION = "stock.consumption";
    static final String PUBLISHED = "restock.events";
    static final String SENT = "notifications.sent";
    static final String FAILED = "notifications.failed";

    private final MeterRegistry meterRegistry;

    public void incrementConsumption() {
        increment(CONSUMPTION);
    }

    public void incrementPublished() {
        increment(PUBLISHED);
    }

    public void incrementSent() {
        increment(SENT);
    }

    public void incrementFailed() {
        increment(FAILED);
    }

    private void increment(String name) {
        try {
            meterRegistry.counter(name).increment();
        } catch (RuntimeException exception) {
            log.error("Falha ao registrar métrica {}", name, exception);
        }
    }
}
