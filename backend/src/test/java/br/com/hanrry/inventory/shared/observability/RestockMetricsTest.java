package br.com.hanrry.inventory.shared.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class RestockMetricsTest {

    private SimpleMeterRegistry registry;
    private RestockMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new RestockMetrics(registry);
    }

    @Test
    void shouldIncrementConsumptionByOne() {
        metrics.incrementConsumption();
        assertEquals(1.0, registry.counter(RestockMetrics.CONSUMPTION).count());
    }

    @Test
    void shouldIncrementPublishedByOne() {
        metrics.incrementPublished();
        assertEquals(1.0, registry.counter(RestockMetrics.PUBLISHED).count());
    }

    @Test
    void shouldIncrementSentByOne() {
        metrics.incrementSent();
        assertEquals(1.0, registry.counter(RestockMetrics.SENT).count());
    }

    @Test
    void shouldIncrementFailedByOne() {
        metrics.incrementFailed();
        assertEquals(1.0, registry.counter(RestockMetrics.FAILED).count());
    }

    @Test
    void shouldNotRethrowWhenRegistryFails(CapturedOutput output) {
        MeterRegistry failingRegistry = mock(MeterRegistry.class);
        when(failingRegistry.counter(RestockMetrics.CONSUMPTION)).thenThrow(new RuntimeException("registry down"));
        RestockMetrics failingMetrics = new RestockMetrics(failingRegistry);

        assertDoesNotThrow(failingMetrics::incrementConsumption);
        assertThat(output.getAll()).contains("Falha ao registrar métrica");
        assertThat(output.getAll()).contains(RestockMetrics.CONSUMPTION);
    }
}
