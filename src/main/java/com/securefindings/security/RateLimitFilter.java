package com.securefindings.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
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
        private static final double TOKEN_EPSILON = 1.0e-9;
        private static final HexFormat FINGERPRINT_FORMAT = HexFormat.of();

        private final RateLimitProperties properties;
        private final Clock clock;
        private final ConcurrentMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
        private final AtomicInteger trackedBucketCount = new AtomicInteger();
        private final AtomicLong requestsSinceCleanup = new AtomicLong();
        private final RedisRateLimitStore redisRateLimitStore;

        public RateLimitFilter(
                        RateLimitProperties properties,
                        Clock clock) {
                this(properties, clock, null);
        }

        RateLimitFilter(
                        RateLimitProperties properties,
                        Clock clock,
                        RedisRateLimitStore redisRateLimitStore) {

                this.properties = Objects.requireNonNull(properties);
                this.clock = Objects.requireNonNull(clock);
                this.redisRateLimitStore = redisRateLimitStore;
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
                String clientKey = clientKey(request);

                RateLimitDecision decision;

                try {
                        decision = redisRateLimitStore == null
                                        ? consume(clientKey, now)
                                        : redisRateLimitStore.consume(
                                                        fingerprintClientKey(clientKey),
                                                        properties);
                } catch (DataAccessException | IllegalStateException exception) {
                        writeRateLimitUnavailable(response);
                        return;
                }

                if (!decision.allowed()) {
                        writeTooManyRequests(response, now, decision);
                        return;
                }

                writeRateLimitHeaders(response, decision);
                filterChain.doFilter(request, response);
        }

        private void writeRateLimitUnavailable(
                        HttpServletResponse response) throws IOException {

                response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.setHeader(HttpHeaders.RETRY_AFTER, "1");
                response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");

                response.getWriter().write("""
                                {
                                    "code": "RATE_LIMIT_UNAVAILABLE",
                                    "message": "El servicio de limitación de peticiones no está disponible",
                                    "errors": {}
                                }
                                """);
        }

        private RateLimitDecision consume(
                        String clientKey,
                        Instant now) {

                AtomicReference<RateLimitDecision> decisionReference = new AtomicReference<>();
                int capacity = properties.maxRequests();
                double windowSeconds = durationInSeconds(properties.window());
                double tokensPerSecond = capacity / windowSeconds;

                buckets.compute(clientKey, (ignored, currentBucket) -> {
                        if (currentBucket == null && !reserveBucket()) {
                                Instant resetAt = instantAfterRefill(
                                                now,
                                                capacity,
                                                tokensPerSecond);

                                decisionReference.set(new RateLimitDecision(
                                                false,
                                                resetAt,
                                                resetAt,
                                                0));
                                return null;
                        }

                        Instant effectiveNow = currentBucket == null
                                        || now.isAfter(currentBucket.lastRefillAt())
                                                        ? now
                                                        : currentBucket.lastRefillAt();

                        double availableTokens = currentBucket == null
                                        ? capacity
                                        : currentBucket.tokens()
                                                        + durationInSeconds(Duration.between(
                                                                        currentBucket.lastRefillAt(),
                                                                        effectiveNow))
                                                                        * tokensPerSecond;

                        availableTokens = normalizeTokens(availableTokens, capacity);

                        boolean allowed = availableTokens >= 1.0 - TOKEN_EPSILON;
                        double remainingTokens = allowed
                                        ? Math.max(0.0, availableTokens - 1.0)
                                        : availableTokens;

                        Instant resetAt = instantAfterRefill(
                                        effectiveNow,
                                        capacity - remainingTokens,
                                        tokensPerSecond);

                        Instant retryAt = allowed
                                        ? effectiveNow
                                        : instantAfterRefill(
                                                        effectiveNow,
                                                        1.0 - remainingTokens,
                                                        tokensPerSecond);

                        decisionReference.set(new RateLimitDecision(
                                        allowed,
                                        resetAt,
                                        retryAt,
                                        allowed
                                                        ? wholeRemainingTokens(remainingTokens)
                                                        : 0));

                        return new TokenBucket(
                                        remainingTokens,
                                        effectiveNow,
                                        resetAt);
                });

                cleanupExpiredBuckets(now);

                return Objects.requireNonNull(
                                decisionReference.get(),
                                "No se pudo calcular el límite de peticiones");
        }

        private double normalizeTokens(
                        double tokens,
                        int capacity) {

                double boundedTokens = Math.max(0.0, Math.min(capacity, tokens));
                double nearestInteger = Math.rint(boundedTokens);

                return Math.abs(boundedTokens - nearestInteger) <= TOKEN_EPSILON
                                ? nearestInteger
                                : boundedTokens;
        }

        private int wholeRemainingTokens(double tokens) {
                return Math.max(
                                0,
                                Math.min(
                                                properties.maxRequests(),
                                                (int) Math.floor(tokens + TOKEN_EPSILON)));
        }

        private double durationInSeconds(Duration duration) {
                return duration.getSeconds()
                                + duration.getNano() / 1_000_000_000.0;
        }

        private Instant instantAfterRefill(
                        Instant reference,
                        double tokensToRefill,
                        double tokensPerSecond) {

                if (tokensToRefill <= 0.0) {
                        return reference;
                }

                double secondsToRefill = tokensToRefill / tokensPerSecond;
                double nearestWholeSecond = Math.rint(secondsToRefill);

                if (Math.abs(secondsToRefill - nearestWholeSecond) <= TOKEN_EPSILON) {
                        secondsToRefill = nearestWholeSecond;
                }

                long wholeSeconds = (long) Math.floor(secondsToRefill);
                long nanoseconds = (long) Math.ceil(
                                (secondsToRefill - wholeSeconds) * 1_000_000_000.0);

                if (nanoseconds >= 1_000_000_000L) {
                        wholeSeconds++;
                        nanoseconds -= 1_000_000_000L;
                }

                return reference.plusSeconds(wholeSeconds).plusNanos(nanoseconds);
        }

        private boolean reserveBucket() {
                while (true) {
                        int trackedBuckets = trackedBucketCount.get();

                        if (trackedBuckets >= properties.maxTrackedClients()) {
                                return false;
                        }

                        if (trackedBucketCount.compareAndSet(
                                        trackedBuckets,
                                        trackedBuckets + 1)) {
                                return true;
                        }
                }
        }

        private void cleanupExpiredBuckets(Instant now) {
                long requests = requestsSinceCleanup.incrementAndGet();

                long cleanupInterval = Math.max(
                                CLEANUP_INTERVAL,
                                trackedBucketCount.get() / 10L);

                if (requests < cleanupInterval
                                || !requestsSinceCleanup.compareAndSet(requests, 0)) {
                        return;
                }

                for (Map.Entry<String, TokenBucket> entry : buckets.entrySet()) {
                        TokenBucket bucket = entry.getValue();

                        if (!now.isBefore(bucket.resetAt())
                                        && buckets.remove(entry.getKey(), bucket)) {
                                trackedBucketCount.decrementAndGet();
                        }
                }
        }

        private String clientKey(HttpServletRequest request) {
                Authentication authentication = SecurityContextHolder
                                .getContext()
                                .getAuthentication();

                if (authentication != null && authentication.isAuthenticated()) {
                        String name = authentication.getName();

                        if (name != null
                                        && !name.isBlank()
                                        && !ANONYMOUS_USER.equals(name)) {
                                return fingerprintClientKey(
                                                authenticatedClientKey(authentication, name));
                        }
                }

                String remoteAddress = request.getRemoteAddr();
                String ipKey = "ip:" + (remoteAddress == null || remoteAddress.isBlank()
                                ? "unknown"
                                : remoteAddress);

                return fingerprintClientKey(ipKey);
        }

        private String authenticatedClientKey(
                        Authentication authentication,
                        String name) {

                if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
                        return "user:" + name;
                }

                String organizationClaim = jwtAuthentication
                                .getToken()
                                .getClaimAsString("organization_id");

                if (organizationClaim == null || organizationClaim.isBlank()) {
                        return "user:" + name;
                }

                try {
                        UUID organizationId = UUID.fromString(organizationClaim);
                        String principalKey = jwtPrincipalKey(jwtAuthentication, name);

                        return "organization:" + organizationId + ":" + principalKey;
                } catch (IllegalArgumentException exception) {
                        return "user:" + name;
                }
        }

        private String jwtPrincipalKey(
                        JwtAuthenticationToken jwtAuthentication,
                        String name) {

                String subject = jwtAuthentication.getToken().getSubject();

                Object issuerClaim = jwtAuthentication
                                .getToken()
                                .getClaims()
                                .get("iss");

                String issuer = issuerClaim == null ? null : issuerClaim.toString();

                if (subject == null || subject.isBlank()
                                || issuer == null || issuer.isBlank()) {
                        return "user:" + name;
                }

                return "issuer:" + issuer + ":subject:" + subject;
        }

        static String fingerprintClientKey(String clientKey) {
                Objects.requireNonNull(clientKey, "La clave del cliente no puede ser nula");

                try {
                        byte[] digest = MessageDigest
                                        .getInstance("SHA-256")
                                        .digest(clientKey.getBytes(StandardCharsets.UTF_8));

                        return FINGERPRINT_FORMAT.formatHex(digest);
                } catch (NoSuchAlgorithmException exception) {
                        throw new IllegalStateException(
                                        "SHA-256 no está disponible para el rate limiter",
                                        exception);
                }
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

                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                writeRateLimitHeaders(response, decision);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());

                response.setHeader(
                                HttpHeaders.RETRY_AFTER,
                                String.valueOf(retryAfterSeconds(now, decision.retryAt())));

                response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");

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
                        Instant retryAt) {

                Duration remaining = Duration.between(now, retryAt);
                long seconds = remaining.getSeconds();

                if (remaining.getNano() > 0) {
                        seconds++;
                }

                return Math.max(1, seconds);
        }

        private record TokenBucket(
                        double tokens,
                        Instant lastRefillAt,
                        Instant resetAt) {
        }
}