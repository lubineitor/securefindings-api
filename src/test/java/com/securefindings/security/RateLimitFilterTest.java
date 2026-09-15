package com.securefindings.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.FilterChain;

class RateLimitFilterTest {

    private static final Instant START = Instant.parse("2026-09-15T00:00:00Z");

    private FilterChain filterChain;
    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void configurarFiltro() {
        filterChain = mock(FilterChain.class);
        rateLimitFilter = createFilter(2);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void limpiarContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deberiaBloquearLaPeticionQueSuperaElLimite()
            throws Exception {

        invoke("/api/v1/findings", "10.0.0.1");
        invoke("/api/v1/findings", "10.0.0.1");

        MockHttpServletResponse limitedResponse = invoke("/api/v1/findings", "10.0.0.1");

        assertEquals(429, limitedResponse.getStatus());
        assertEquals(
                "60",
                limitedResponse.getHeader("Retry-After"));
        assertTrue(
                limitedResponse.getContentAsString()
                        .contains("RATE_LIMIT_EXCEEDED"));

        verify(filterChain, times(2))
                .doFilter(any(), any());
    }

    @Test
    void deberiaMantenerUnLimiteIndependientePorDireccionIp()
            throws Exception {

        rateLimitFilter = createFilter(1);

        MockHttpServletResponse firstResponse = invoke("/api/v1/findings", "10.0.0.1");

        MockHttpServletResponse repeatedResponse = invoke("/api/v1/findings", "10.0.0.1");

        MockHttpServletResponse otherClientResponse = invoke("/api/v1/findings", "10.0.0.2");

        assertEquals(200, firstResponse.getStatus());
        assertEquals(429, repeatedResponse.getStatus());
        assertEquals(200, otherClientResponse.getStatus());

        verify(filterChain, times(2))
                .doFilter(any(), any());
    }

    @Test
    void deberiaMantenerUnLimiteIndependientePorUsuario()
            throws Exception {

        rateLimitFilter = createFilter(1);

        autenticarComo("analista");

        MockHttpServletResponse firstUserResponse = invoke("/api/v1/findings", "10.0.0.1");

        MockHttpServletResponse repeatedUserResponse = invoke("/api/v1/findings", "10.0.0.1");

        autenticarComo("administrador");

        MockHttpServletResponse otherUserResponse = invoke("/api/v1/findings", "10.0.0.1");

        assertEquals(200, firstUserResponse.getStatus());
        assertEquals(429, repeatedUserResponse.getStatus());
        assertEquals(200, otherUserResponse.getStatus());

        verify(filterChain, times(2))
                .doFilter(any(), any());
    }

    @Test
    void noDebeAplicarElLimiteAlHealthCheck()
            throws Exception {

        rateLimitFilter = createFilter(1);

        MockHttpServletResponse firstResponse = invoke("/api/v1/health", "10.0.0.1");

        MockHttpServletResponse secondResponse = invoke("/api/v1/health", "10.0.0.1");

        assertEquals(200, firstResponse.getStatus());
        assertEquals(200, secondResponse.getStatus());

        verify(filterChain, times(2))
                .doFilter(any(), any());
    }

    private RateLimitFilter createFilter(int maxRequests) {
        return new RateLimitFilter(
                new RateLimitProperties(
                        maxRequests,
                        Duration.ofMinutes(1)),
                Clock.fixed(
                        START,
                        ZoneOffset.UTC));
    }

    private MockHttpServletResponse invoke(
            String path,
            String remoteAddress)
            throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setMethod("GET");
        request.setRequestURI(path);
        request.setRemoteAddr(remoteAddress);

        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitFilter.doFilter(
                request,
                response,
                filterChain);

        return response;
    }

    private void autenticarComo(String username) {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new TestingAuthenticationToken(
                                username,
                                "credentials",
                                "ROLE_ANALYST"));
    }
}