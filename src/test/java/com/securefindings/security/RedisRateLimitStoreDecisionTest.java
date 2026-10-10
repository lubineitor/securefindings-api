package com.securefindings.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RedisRateLimitStoreDecisionTest {

    @Test
    void deberiaInterpretarUnaDecisionPermitida() {
        RateLimitDecision decision = RedisRateLimitStore.parseDecision(
                "1:4:1790000001000:1790000000000");

        assertTrue(decision.allowed());
        assertEquals(4, decision.remaining());
        assertEquals(
                Instant.ofEpochMilli(1790000001000L),
                decision.resetAt());
        assertEquals(
                Instant.ofEpochMilli(1790000000000L),
                decision.retryAt());
    }

    @Test
    void deberiaInterpretarUnaDecisionRechazada() {
        RateLimitDecision decision = RedisRateLimitStore.parseDecision(
                "0:0:1790000001000:1790000001000");

        assertFalse(decision.allowed());
        assertEquals(0, decision.remaining());
    }

    @Test
    void deberiaRechazarUnResultadoNulo() {
        assertThrows(
                IllegalStateException.class,
                () -> RedisRateLimitStore.parseDecision(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "1:0:1000",
            "1:abc:1000:1000",
            "2:0:1000:1000",
            "1:-1:1000:1000",
            "1:0:-1:1000",
            "1:0:1000:-1",
            "1:0:1000:2000"
    })
    void deberiaRechazarUnResultadoRedisInvalido(String result) {
        assertThrows(
                IllegalStateException.class,
                () -> RedisRateLimitStore.parseDecision(result));
    }
}