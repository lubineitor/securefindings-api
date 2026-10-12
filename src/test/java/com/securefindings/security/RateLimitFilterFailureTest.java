package com.securefindings.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterFailureTest {

    private final RateLimitProperties properties = new RateLimitProperties(5, Duration.ofMinutes(1), 100);

    @Test
    void deberiaResponder503SiRedisDevuelveUnResultadoInvalido()
            throws Exception {

        assertRedisFailureIsClosed(
                new IllegalStateException(
                        "Redis devolvió un resultado inválido"));
    }

    @Test
    void deberiaResponder503SiRedisNoEstaDisponible()
            throws Exception {

        assertRedisFailureIsClosed(
                new DataAccessResourceFailureException(
                        "Redis no está disponible"));
    }

    private void assertRedisFailureIsClosed(RuntimeException failure)
            throws Exception {

        RedisRateLimitStore redisRateLimitStore = mock(RedisRateLimitStore.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(redisRateLimitStore.consume(
                anyString(),
                eq(properties)))
                .thenThrow(failure);

        RateLimitFilter filter = new RateLimitFilter(
                properties,
                Clock.systemUTC(),
                redisRateLimitStore);

        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/api/v1/findings");
        request.setRemoteAddr("192.0.2.10");

        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                response.getStatus());
        assertEquals("1", response.getHeader("Retry-After"));
        assertEquals("no-store", response.getHeader("Cache-Control"));
        assertTrue(
                response.getContentAsString()
                        .contains("RATE_LIMIT_UNAVAILABLE"));

        verifyNoInteractions(filterChain);
    }
}