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
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import jakarta.servlet.FilterChain;

class RateLimitFilterTest {

        private static final Instant START = Instant.parse("2026-09-15T00:00:00Z");

        private FilterChain filterChain;
        private RateLimitFilter rateLimitFilter;
        private MutableClock clock;

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
        void deberiaExponerElEstadoDelLimiteEnRespuestasPermitidas()
                        throws Exception {

                MockHttpServletResponse response = invoke(
                                "/api/v1/findings",
                                "10.0.0.1");

                assertEquals(200, response.getStatus());
                assertEquals(
                                "2",
                                response.getHeader("X-RateLimit-Limit"));
                assertEquals(
                                "1",
                                response.getHeader("X-RateLimit-Remaining"));
                assertEquals(
                                String.valueOf(START.plusSeconds(30).getEpochSecond()),
                                response.getHeader("X-RateLimit-Reset"));
        }

        @Test
        void deberiaBloquearLaPeticionQueSuperaElLimite()
                        throws Exception {

                invoke("/api/v1/findings", "10.0.0.1");
                invoke("/api/v1/findings", "10.0.0.1");

                MockHttpServletResponse limitedResponse = invoke(
                                "/api/v1/findings",
                                "10.0.0.1");

                assertEquals(429, limitedResponse.getStatus());
                assertEquals(
                                "30",
                                limitedResponse.getHeader("Retry-After"));
                assertEquals(
                                "2",
                                limitedResponse.getHeader("X-RateLimit-Limit"));
                assertEquals(
                                "0",
                                limitedResponse.getHeader("X-RateLimit-Remaining"));
                assertEquals(
                                String.valueOf(START.plusSeconds(60).getEpochSecond()),
                                limitedResponse.getHeader("X-RateLimit-Reset"));
                assertTrue(
                                limitedResponse.getContentAsString()
                                                .contains("RATE_LIMIT_EXCEEDED"));

                verify(filterChain, times(2))
                                .doFilter(any(), any());
        }

        @Test
        void deberiaReponerGradualmenteLosTokens()
                        throws Exception {

                rateLimitFilter = createFilter(2);

                invoke("/api/v1/findings", "10.0.0.1");
                invoke("/api/v1/findings", "10.0.0.1");

                clock.advance(Duration.ofSeconds(30));

                MockHttpServletResponse refilledResponse = invoke(
                                "/api/v1/findings",
                                "10.0.0.1");

                assertEquals(200, refilledResponse.getStatus());
                assertEquals(
                                "0",
                                refilledResponse.getHeader("X-RateLimit-Remaining"));
                assertEquals(
                                String.valueOf(START.plusSeconds(90).getEpochSecond()),
                                refilledResponse.getHeader("X-RateLimit-Reset"));

                MockHttpServletResponse limitedResponse = invoke(
                                "/api/v1/findings",
                                "10.0.0.1");

                assertEquals(429, limitedResponse.getStatus());
                assertEquals(
                                "30",
                                limitedResponse.getHeader("Retry-After"));
                assertEquals(
                                String.valueOf(START.plusSeconds(90).getEpochSecond()),
                                limitedResponse.getHeader("X-RateLimit-Reset"));

                verify(filterChain, times(3))
                                .doFilter(any(), any());
        }

        @Test
        void noDebeSuperarLaCuotaConPeticionesConcurrentes()
                        throws Exception {

                int maxRequests = 5;
                int totalRequests = 20;
                rateLimitFilter = createFilter(maxRequests);

                ExecutorService executor = Executors.newFixedThreadPool(totalRequests);
                CountDownLatch ready = new CountDownLatch(totalRequests);
                CountDownLatch start = new CountDownLatch(1);
                List<Future<Integer>> responses = new ArrayList<>(totalRequests);

                try {
                        for (int i = 0; i < totalRequests; i++) {
                                responses.add(executor.submit(() -> {
                                        ready.countDown();

                                        if (!start.await(10, TimeUnit.SECONDS)) {
                                                throw new IllegalStateException(
                                                                "No se inició la ráfaga concurrente");
                                        }

                                        return invoke(
                                                        "/api/v1/findings",
                                                        "10.0.0.1")
                                                        .getStatus();
                                }));
                        }

                        assertTrue(
                                        ready.await(10, TimeUnit.SECONDS),
                                        "Todas las peticiones deben esperar en la barrera");

                        start.countDown();

                        int allowedRequests = 0;
                        int rejectedRequests = 0;

                        for (Future<Integer> response : responses) {
                                int status = response.get(10, TimeUnit.SECONDS);

                                if (status == 200) {
                                        allowedRequests++;
                                } else if (status == 429) {
                                        rejectedRequests++;
                                } else {
                                        throw new AssertionError(
                                                        "Estado HTTP inesperado: " + status);
                                }
                        }

                        assertEquals(maxRequests, allowedRequests);
                        assertEquals(
                                        totalRequests - maxRequests,
                                        rejectedRequests);

                        verify(filterChain, times(maxRequests))
                                        .doFilter(any(), any());
                } finally {
                        start.countDown();
                        executor.shutdownNow();
                        assertTrue(
                                        executor.awaitTermination(10, TimeUnit.SECONDS),
                                        "El executor debe finalizar");
                }
        }

        @Test
        void deberiaMantenerUnLimiteIndependientePorDireccionIp()
                        throws Exception {

                rateLimitFilter = createFilter(1);

                MockHttpServletResponse firstResponse = invoke(
                                "/api/v1/findings",
                                "10.0.0.1");

                MockHttpServletResponse repeatedResponse = invoke(
                                "/api/v1/findings",
                                "10.0.0.1");

                MockHttpServletResponse otherClientResponse = invoke(
                                "/api/v1/findings",
                                "10.0.0.2");

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

                MockHttpServletResponse firstUserResponse = invoke(
                                "/api/v1/findings",
                                "10.0.0.1");

                MockHttpServletResponse repeatedUserResponse = invoke(
                                "/api/v1/findings",
                                "10.0.0.1");

                autenticarComo("administrador");

                MockHttpServletResponse otherUserResponse = invoke(
                                "/api/v1/findings",
                                "10.0.0.1");

                assertEquals(200, firstUserResponse.getStatus());
                assertEquals(429, repeatedUserResponse.getStatus());
                assertEquals(200, otherUserResponse.getStatus());

                verify(filterChain, times(2))
                                .doFilter(any(), any());
        }

        @Test
        void deberiaSepararLosLimitesPorOrganizacionParaElMismoUsuario()
                        throws Exception {

                rateLimitFilter = createFilter(1);

                autenticarConOrganizacion(
                                "analista",
                                UUID.fromString("00000000-0000-0000-0000-000000000001"));

                MockHttpServletResponse primeraOrganizacion = invoke(
                                "/api/v1/findings",
                                "10.0.0.1");

                MockHttpServletResponse mismaOrganizacion = invoke(
                                "/api/v1/findings",
                                "10.0.0.1");

                autenticarConOrganizacion(
                                "analista",
                                UUID.fromString("00000000-0000-0000-0000-000000000002"));

                MockHttpServletResponse segundaOrganizacion = invoke(
                                "/api/v1/findings",
                                "10.0.0.1");

                assertEquals(200, primeraOrganizacion.getStatus());
                assertEquals(429, mismaOrganizacion.getStatus());
                assertEquals(200, segundaOrganizacion.getStatus());

                verify(filterChain, times(2))
                                .doFilter(any(), any());
        }

        @Test
        void deberiaSepararLasCuotasPorSubjectAunqueCoincidaElNombre()
                        throws Exception {

                rateLimitFilter = createFilter(1);
                String organizationId = "00000000-0000-0000-0000-000000000001";

                autenticarConClaimOrganizacion(
                                "analista",
                                "subject-analista-1",
                                organizationId);

                MockHttpServletResponse primeraIdentidad = invoke(
                                "/api/v1/findings",
                                "10.0.0.1");

                autenticarConClaimOrganizacion(
                                "analista",
                                "subject-analista-2",
                                organizationId);

                MockHttpServletResponse segundaIdentidad = invoke(
                                "/api/v1/findings",
                                "10.0.0.1");

                assertEquals(200, primeraIdentidad.getStatus());
                assertEquals(200, segundaIdentidad.getStatus());

                verify(filterChain, times(2))
                                .doFilter(any(), any());
        }

        @Test
        void deberiaConservarLaCuotaSiCambiaElNombreDelMismoSubject()
                        throws Exception {

                rateLimitFilter = createFilter(1);
                String organizationId = "00000000-0000-0000-0000-000000000001";
                String subject = "subject-analista-estable";

                autenticarConClaimOrganizacion(
                                "analista",
                                subject,
                                organizationId);

                MockHttpServletResponse primeraPeticion = invoke(
                                "/api/v1/findings",
                                "10.0.0.1");

                autenticarConClaimOrganizacion(
                                "analista-renombrado",
                                subject,
                                organizationId);

                MockHttpServletResponse peticionTrasRenombre = invoke(
                                "/api/v1/findings",
                                "10.0.0.1");

                assertEquals(200, primeraPeticion.getStatus());
                assertEquals(429, peticionTrasRenombre.getStatus());

                verify(filterChain)
                                .doFilter(any(), any());
        }

        @Test
        void unClaimDeOrganizacionInvalidoNoDebeCrearUnaCuotaNueva()
                        throws Exception {

                rateLimitFilter = createFilter(1);

                autenticarComo("analista");

                MockHttpServletResponse primeraPeticion = invoke(
                                "/api/v1/findings",
                                "10.0.0.1");

                autenticarConClaimOrganizacion(
                                "analista",
                                "organizacion-invalida");

                MockHttpServletResponse segundaPeticion = invoke(
                                "/api/v1/findings",
                                "10.0.0.1");

                assertEquals(200, primeraPeticion.getStatus());
                assertEquals(429, segundaPeticion.getStatus());

                verify(filterChain)
                                .doFilter(any(), any());
        }

        @Test
        void noDebeAplicarElLimiteAlHealthCheck()
                        throws Exception {

                rateLimitFilter = createFilter(1);

                MockHttpServletResponse firstResponse = invoke(
                                "/api/v1/health",
                                "10.0.0.1");

                MockHttpServletResponse secondResponse = invoke(
                                "/api/v1/health",
                                "10.0.0.1");

                assertEquals(200, firstResponse.getStatus());
                assertEquals(200, secondResponse.getStatus());

                verify(filterChain, times(2))
                                .doFilter(any(), any());
        }

        private RateLimitFilter createFilter(int maxRequests) {
                clock = new MutableClock(START);

                return new RateLimitFilter(
                                new RateLimitProperties(
                                                maxRequests,
                                                Duration.ofMinutes(1)),
                                clock);
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

        private void autenticarConOrganizacion(
                        String username,
                        UUID organizationId) {

                autenticarConClaimOrganizacion(
                                username,
                                organizationId.toString());
        }

        private void autenticarConClaimOrganizacion(
                        String username,
                        String organizationClaim) {

                autenticarConClaimOrganizacion(
                                username,
                                username,
                                organizationClaim);
        }

        private void autenticarConClaimOrganizacion(
                        String username,
                        String subject,
                        String organizationClaim) {

                Jwt jwt = Jwt.withTokenValue("token-de-prueba-" + organizationClaim + "-" + subject)
                                .header("alg", "none")
                                .subject(subject)
                                .claim("iss", "https://issuer.example.test/realms/securefindings")
                                .claim("preferred_username", username)
                                .claim("organization_id", organizationClaim)
                                .issuedAt(START.minusSeconds(60))
                                .expiresAt(START.plusSeconds(300))
                                .build();

                SecurityContextHolder.getContext()
                                .setAuthentication(new JwtAuthenticationToken(
                                                jwt,
                                                List.of(),
                                                username));
        }

        private void autenticarComo(String username) {
                SecurityContextHolder.getContext()
                                .setAuthentication(
                                                new TestingAuthenticationToken(
                                                                username,
                                                                "credentials",
                                                                "ROLE_ANALYST"));
        }

        private static final class MutableClock extends Clock {

                private final AtomicReference<Instant> currentInstant;
                private final ZoneId zone;

                private MutableClock(Instant initialInstant) {
                        this(new AtomicReference<>(initialInstant), ZoneOffset.UTC);
                }

                private MutableClock(
                                AtomicReference<Instant> currentInstant,
                                ZoneId zone) {

                        this.currentInstant = currentInstant;
                        this.zone = zone;
                }

                private void advance(Duration duration) {
                        currentInstant.updateAndGet(instant -> instant.plus(duration));
                }

                @Override
                public ZoneId getZone() {
                        return zone;
                }

                @Override
                public Clock withZone(ZoneId zone) {
                        return new MutableClock(currentInstant, zone);
                }

                @Override
                public Instant instant() {
                        return currentInstant.get();
                }
        }
}
