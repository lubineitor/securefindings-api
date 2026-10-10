package com.securefindings.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterRedisTest {

    private static final Instant NOW = Instant.parse("2026-10-11T00:00:00Z");

    private static final String REMOTE_ADDRESS = "192.0.2.10";

    @Mock
    private RedisRateLimitStore redisRateLimitStore;

    private final RateLimitProperties properties = new RateLimitProperties(5, Duration.ofMinutes(1), 100);

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        filter = new RateLimitFilter(
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC),
                redisRateLimitStore);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deberiaContinuarLaPeticionCuandoRedisLaPermite()
            throws Exception {

        when(redisRateLimitStore.consume(
                anyString(),
                eq(properties)))
                .thenReturn(new RateLimitDecision(
                        true,
                        NOW.plusSeconds(60),
                        NOW,
                        4));

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(
                newRequest(),
                response,
                filterChain);

        assertEquals(200, response.getStatus());
        assertEquals("5", response.getHeader("X-RateLimit-Limit"));
        assertEquals("4", response.getHeader("X-RateLimit-Remaining"));
        assertTrue(response.getHeader("X-RateLimit-Reset") != null);
        assertTrue(filterChain.getRequest() != null);

        ArgumentCaptor<String> fingerprintCaptor = ArgumentCaptor.forClass(String.class);

        verify(redisRateLimitStore).consume(
                fingerprintCaptor.capture(),
                eq(properties));

        assertTrue(fingerprintCaptor.getValue().matches("[0-9a-f]{64}"));
    }

    @Test
    void deberiaDevolver429CuandoRedisDeniegaLaPeticion()
            throws Exception {

        when(redisRateLimitStore.consume(
                anyString(),
                eq(properties)))
                .thenReturn(new RateLimitDecision(
                        false,
                        NOW.plusSeconds(60),
                        NOW.plusSeconds(15),
                        0));

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(
                newRequest(),
                response,
                filterChain);

        assertEquals(429, response.getStatus());
        assertEquals("0", response.getHeader("X-RateLimit-Remaining"));
        assertEquals("15", response.getHeader("Retry-After"));
        assertTrue(response.getContentAsString()
                .contains("\"code\": \"RATE_LIMIT_EXCEEDED\""));
        assertNull(filterChain.getRequest());
    }

    @Test
    void deberiaDevolver503YNoContinuarSiRedisNoEstaDisponible()
            throws Exception {

        when(redisRateLimitStore.consume(
                anyString(),
                eq(properties)))
                .thenThrow(new DataAccessResourceFailureException(
                        "Redis no disponible"));

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(
                newRequest(),
                response,
                filterChain);

        assertEquals(503, response.getStatus());
        assertEquals("1", response.getHeader("Retry-After"));
        assertEquals("no-store", response.getHeader("Cache-Control"));
        assertTrue(response.getContentAsString()
                .contains("\"code\": \"RATE_LIMIT_UNAVAILABLE\""));
        assertNull(filterChain.getRequest());
    }

    private MockHttpServletRequest newRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/api/v1/findings");

        request.setRemoteAddr(REMOTE_ADDRESS);

        return request;
    }
}