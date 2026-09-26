package com.securefindings.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class RateLimitFilterConcurrencyTest {

    private static final Instant START = Instant.parse("2026-09-15T00:00:00Z");
    private static final String REQUEST_URI = "/api/v1/findings";
    private static final String REMOTE_ADDRESS = "10.0.0.1";

    @Test
    void noDebeSuperarLaCuotaAntePeticionesConcurrentes()
            throws Exception {

        int maxRequests = 8;
        int concurrentRequests = 64;

        RateLimitFilter filter = new RateLimitFilter(
                new RateLimitProperties(
                        maxRequests,
                        Duration.ofMinutes(1)),
                Clock.fixed(
                        START,
                        ZoneOffset.UTC));

        AtomicInteger requestsPassedToChain = new AtomicInteger();
        CountDownLatch ready = new CountDownLatch(concurrentRequests);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> responses = new ArrayList<>(concurrentRequests);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            while (responses.size() < concurrentRequests) {
                responses.add(executor.submit(() -> {
                    SecurityContextHolder.clearContext();

                    try {
                        MockHttpServletRequest request = new MockHttpServletRequest();
                        request.setMethod("GET");
                        request.setRequestURI(REQUEST_URI);
                        request.setRemoteAddr(REMOTE_ADDRESS);

                        MockHttpServletResponse response = new MockHttpServletResponse();

                        ready.countDown();

                        if (!start.await(10, TimeUnit.SECONDS)) {
                            throw new IllegalStateException(
                                    "No se inició la prueba concurrente");
                        }

                        filter.doFilter(
                                request,
                                response,
                                (servletRequest, servletResponse) -> requestsPassedToChain
                                        .incrementAndGet());

                        return response.getStatus();
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                }));
            }

            boolean allRequestsReady = ready.await(10, TimeUnit.SECONDS);
            start.countDown();

            assertTrue(
                    allRequestsReady,
                    "Todas las peticiones deben esperar antes de comenzar");

            List<Integer> statuses = new ArrayList<>(concurrentRequests);

            for (Future<Integer> response : responses) {
                statuses.add(response.get(10, TimeUnit.SECONDS));
            }

            long successfulResponses = statuses.stream()
                    .filter(status -> status == 200)
                    .count();

            long limitedResponses = statuses.stream()
                    .filter(status -> status == 429)
                    .count();

            assertEquals(maxRequests, successfulResponses);
            assertEquals(
                    concurrentRequests - maxRequests,
                    limitedResponses);
            assertEquals(maxRequests, requestsPassedToChain.get());
        }
    }
}