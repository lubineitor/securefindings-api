package com.securefindings.security;

import java.time.Instant;

record RateLimitDecision(
        boolean allowed,
        Instant resetAt,
        Instant retryAt,
        int remaining) {
}