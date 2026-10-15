package com.securefindings.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class MicrometerRateLimitMetricsTest {

    @Test
    void deberiaContarLosResultadosConEtiquetasDeCardinalidadBaja() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerRateLimitMetrics metrics = new MicrometerRateLimitMetrics(registry);

        metrics.recordAllowed();
        metrics.recordAllowed();
        metrics.recordRateLimited();
        metrics.recordUnavailable();

        assertEquals(
                2.0,
                registry.get("securefindings.rate_limit.requests")
                        .tag("outcome", "allowed")
                        .counter()
                        .count(),
                0.0);

        assertEquals(
                1.0,
                registry.get("securefindings.rate_limit.requests")
                        .tag("outcome", "rate_limited")
                        .counter()
                        .count(),
                0.0);

        assertEquals(
                1.0,
                registry.get("securefindings.rate_limit.requests")
                        .tag("outcome", "unavailable")
                        .counter()
                        .count(),
                0.0);
    }
}
