package com.securefindings.security;

interface RateLimitMetrics {

    RateLimitMetrics NOOP = new RateLimitMetrics() {
    };

    default void recordAllowed() {
    }

    default void recordRateLimited() {
    }

    default void recordUnavailable() {
    }
}