package com.securefindings.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;

class RateLimitFilterMetricsTest {

    private static final Instant NOW = Instant.parse("2026-10-15T00:00:00Z");

    @BeforeEach
    void limpiarContextoAntesDeLaPrueba() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void limpiarContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deberiaContarPeticionesPermitidasYLimitadas()
            throws Exception {

        RateLimitProperties properties = new RateLimitProperties(
                1,
                Duration.ofMinutes(1),
                10);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerRateLimitMetrics metrics = new MicrometerRateLimitMetrics(registry);
        RateLimitFilter filter = new RateLimitFilter(
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC),
                null,
                metrics);
        FilterChain filterChain = mock(FilterChain.class);

        MockHttpServletResponse allowedResponse = invoke(filter, filterChain);
        MockHttpServletResponse limitedResponse = invoke(filter, filterChain);

        assertEquals(200, allowedResponse.getStatus());
        assertEquals(429, limitedResponse.getStatus());
        assertEquals(1.0, metricCount(registry, "allowed"), 0.0);
        assertEquals(1.0, metricCount(registry, "rate_limited"), 0.0);
        assertEquals(0.0, metricCount(registry, "unavailable"), 0.0);
    }

    @Test
    void deberiaContarLasPeticionesRechazadasPorFalloDeRedis()
            throws Exception {

        RateLimitProperties properties = new RateLimitProperties(
                5,
                Duration.ofMinutes(1),
                10);
        RedisRateLimitStore redisRateLimitStore = mock(RedisRateLimitStore.class);
        when(redisRateLimitStore.consume(anyString(), eq(properties)))
                .thenThrow(new DataAccessResourceFailureException(
                        "Redis no está disponible"));

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerRateLimitMetrics metrics = new MicrometerRateLimitMetrics(registry);
        RateLimitFilter filter = new RateLimitFilter(
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC),
                redisRateLimitStore,
                metrics);

        MockHttpServletResponse response = invoke(filter, mock(FilterChain.class));

        assertEquals(503, response.getStatus());
        assertEquals(0.0, metricCount(registry, "allowed"), 0.0);
        assertEquals(0.0, metricCount(registry, "rate_limited"), 0.0);
        assertEquals(1.0, metricCount(registry, "unavailable"), 0.0);
    }

    private MockHttpServletResponse invoke(
            RateLimitFilter filter,
            FilterChain filterChain)
            throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/api/v1/findings");
        request.setRemoteAddr("192.0.2.10");

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, filterChain);
        return response;
    }

    private double metricCount(
            MeterRegistry registry,
            String outcome) {

        return registry.get("securefindings.rate_limit.requests")
                .tag("outcome", outcome)
                .counter()
                .count();
    }
}