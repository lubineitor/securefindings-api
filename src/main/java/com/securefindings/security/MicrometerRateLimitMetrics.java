package com.securefindings.security;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Component
final class MicrometerRateLimitMetrics implements RateLimitMetrics {

    private static final String METRIC_NAME = "securefindings.rate_limit.requests";

    private final Counter allowedRequests;
    private final Counter rateLimitedRequests;
    private final Counter unavailableRequests;

    MicrometerRateLimitMetrics(MeterRegistry meterRegistry) {
        this.allowedRequests = createCounter(meterRegistry, "allowed");
        this.rateLimitedRequests = createCounter(meterRegistry, "rate_limited");
        this.unavailableRequests = createCounter(meterRegistry, "unavailable");
    }

    @Override
    public void recordAllowed() {
        allowedRequests.increment();
    }

    @Override
    public void recordRateLimited() {
        rateLimitedRequests.increment();
    }

    @Override
    public void recordUnavailable() {
        unavailableRequests.increment();
    }

    private Counter createCounter(
            MeterRegistry meterRegistry,
            String outcome) {

        return Counter.builder(METRIC_NAME)
                .description("Resultados del rate limiter")
                .tag("outcome", outcome)
                .register(meterRegistry);
    }
}