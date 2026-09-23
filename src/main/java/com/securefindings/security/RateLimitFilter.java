package com.securefindings.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class RateLimitFilter extends OncePerRequestFilter {

        private static final String API_PREFIX = "/api/v1/";
        private static final String HEALTH_PATH = "/api/v1/health";
        private static final String ANONYMOUS_USER = "anonymousUser";
        private static final String RATE_LIMIT_LIMIT_HEADER = "X-RateLimit-Limit";
        private static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";
        private static final String RATE_LIMIT_RESET_HEADER = "X-RateLimit-Reset";
        private static final int CLEANUP_INTERVAL = 1_000;

        private final RateLimitProperties properties;
        private final Clock clock;
        private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();
        private final AtomicLong requestsSinceCleanup = new AtomicLong();

        public RateLimitFilter(
                        RateLimitProperties properties,
                        Clock clock) {

                this.properties = Objects.requireNonNull(properties);
                this.clock = Objects.requireNonNull(clock);
        }

        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
                String requestUri = request.getRequestURI();

                if (requestUri == null) {
                        return true;
                }

                String contextPath = request.getContextPath();
                String path = requestUri.substring(contextPath.length());

                return !path.startsWith(API_PREFIX)
                                || HEALTH_PATH.equals(path);
        }

        @Override
        protected void doFilterInternal(
                        HttpServletRequest request,
                        HttpServletResponse response,
                        FilterChain filterChain)
                        throws ServletException, IOException {

                Instant now = clock.instant();

                RateLimitDecision decision = consume(
                                clientKey(request),
                                now);

                if (!decision.allowed()) {
                        writeTooManyRequests(
                                        response,
                                        now,
                                        decision);
                        return;
                }

                writeRateLimitHeaders(response, decision);
                filterChain.doFilter(request, response);
        }

        private RateLimitDecision consume(
                        String clientKey,
                        Instant now) {

                AtomicReference<RateLimitDecision> decisionReference = new AtomicReference<>();

                windows.compute(clientKey, (ignored, currentWindow) -> {
                        if (currentWindow == null
                                        || !now.isBefore(currentWindow.resetAt())) {

                                Instant resetAt = now.plus(properties.window());
                                int remaining = properties.maxRequests() - 1;

                                decisionReference.set(
                                                new RateLimitDecision(
                                                                true,
                                                                resetAt,
                                                                remaining));

                                return new Window(1, resetAt);
                        }

                        if (currentWindow.requests() >= properties.maxRequests()) {

                                decisionReference.set(
                                                new RateLimitDecision(
                                                                false,
                                                                currentWindow.resetAt(),
                                                                0));

                                return currentWindow;
                        }

                        Window updatedWindow = new Window(
                                        currentWindow.requests() + 1,
                                        currentWindow.resetAt());

                        int remaining = properties.maxRequests()
                                        - updatedWindow.requests();

                        decisionReference.set(
                                        new RateLimitDecision(
                                                        true,
                                                        currentWindow.resetAt(),
                                                        remaining));

                        return updatedWindow;
                });

                cleanupExpiredWindows(now);

                return Objects.requireNonNull(
                                decisionReference.get(),
                                "No se pudo calcular el límite de peticiones");
        }

        private void cleanupExpiredWindows(Instant now) {
                long requests = requestsSinceCleanup.incrementAndGet();

                if (requests < CLEANUP_INTERVAL
                                || !requestsSinceCleanup.compareAndSet(
                                                requests,
                                                0)) {
                        return;
                }

                windows.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().resetAt()));
        }

        private String clientKey(HttpServletRequest request) {
                Authentication authentication = SecurityContextHolder
                                .getContext()
                                .getAuthentication();

                if (authentication != null
                                && authentication.isAuthenticated()) {

                        String name = authentication.getName();

                        if (name != null
                                        && !name.isBlank()
                                        && !ANONYMOUS_USER.equals(name)) {
                                return "user:" + name;
                        }
                }

                String remoteAddress = request.getRemoteAddr();

                return "ip:" + (remoteAddress == null
                                || remoteAddress.isBlank()
                                                ? "unknown"
                                                : remoteAddress);
        }

        private void writeRateLimitHeaders(
                        HttpServletResponse response,
                        RateLimitDecision decision) {

                response.setHeader(
                                RATE_LIMIT_LIMIT_HEADER,
                                String.valueOf(properties.maxRequests()));

                response.setHeader(
                                RATE_LIMIT_REMAINING_HEADER,
                                String.valueOf(decision.remaining()));

                response.setHeader(
                                RATE_LIMIT_RESET_HEADER,
                                String.valueOf(decision.resetAt().getEpochSecond()));
        }

        private void writeTooManyRequests(
                        HttpServletResponse response,
                        Instant now,
                        RateLimitDecision decision)
                        throws IOException {

                response.setStatus(
                                HttpStatus.TOO_MANY_REQUESTS.value());

                writeRateLimitHeaders(response, decision);

                response.setContentType(
                                MediaType.APPLICATION_JSON_VALUE);

                response.setCharacterEncoding(
                                StandardCharsets.UTF_8.name());

                response.setHeader(
                                HttpHeaders.RETRY_AFTER,
                                String.valueOf(
                                                retryAfterSeconds(now, decision.resetAt())));

                response.setHeader(
                                HttpHeaders.CACHE_CONTROL,
                                "no-store");

                response.getWriter().write("""
                                {
                                    "code": "RATE_LIMIT_EXCEEDED",
                                    "message": "Se ha superado el límite de peticiones",
                                    "errors": {}
                                }
                                """);
        }

        private long retryAfterSeconds(
                        Instant now,
                        Instant resetAt) {

                Duration remaining = Duration.between(
                                now,
                                resetAt);

                long seconds = remaining.getSeconds();

                if (remaining.getNano() > 0) {
                        seconds++;
                }

                return Math.max(1, seconds);
        }

        private record Window(
                        int requests,
                        Instant resetAt) {
        }

        private record RateLimitDecision(
                        boolean allowed,
                        Instant resetAt,
                        int remaining) {
        }
}
