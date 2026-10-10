package com.securefindings.security;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

@Component
final class RedisRateLimitStore {

    private static final String DEFAULT_KEY_PREFIX = "securefindings:rate-limit";
    private static final String CLIENT_FINGERPRINT_PATTERN = "[0-9a-f]{64}";

    private static final String SCRIPT_SOURCE = """
            local time = redis.call('TIME')
            local now = tonumber(time[1]) * 1000
                    + math.floor(tonumber(time[2]) / 1000)

            local capacity = tonumber(ARGV[1])
            local windowMillis = tonumber(ARGV[2])
            local maxClients = tonumber(ARGV[3])
            local member = ARGV[4]
            local tokensPerMillisecond = capacity / windowMillis

            local function result(allowed, remaining, resetAt, retryAt)
                return string.format(
                    '%.0f:%.0f:%.0f:%.0f',
                    allowed,
                    remaining,
                    resetAt,
                    retryAt)
            end

            redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', now)

            local registeredScore = redis.call('ZSCORE', KEYS[1], member)
            local storedTokens = redis.call('HGET', KEYS[2], 'tokens')
            local tokens
            local lastRefillAt

            if not storedTokens or not registeredScore then
                if registeredScore then
                    redis.call('ZREM', KEYS[1], member)
                end

                if storedTokens then
                    redis.call('DEL', KEYS[2])
                end

                if tonumber(redis.call('ZCARD', KEYS[1])) >= maxClients then
                    local oldest = redis.call(
                        'ZRANGE',
                        KEYS[1],
                        0,
                        0,
                        'WITHSCORES')

                    local retryAt = now + windowMillis

                    if oldest[2] then
                        retryAt = tonumber(oldest[2])
                    end

                    return result(0, 0, retryAt, retryAt)
                end

                tokens = capacity
                lastRefillAt = now
            else
                tokens = tonumber(storedTokens)
                lastRefillAt = tonumber(
                    redis.call('HGET', KEYS[2], 'lastRefillAt')) or now

                local effectiveNow = math.max(now, lastRefillAt)
                local elapsedMillis = effectiveNow - lastRefillAt

                tokens = math.min(
                    capacity,
                    tokens + elapsedMillis * tokensPerMillisecond)

                now = effectiveNow
                lastRefillAt = effectiveNow
            end

            local allowed = 0

            if tokens >= 1 then
                allowed = 1
                tokens = tokens - 1
            end

            tokens = math.max(0, math.min(capacity, tokens))

            local remaining = math.floor(tokens + 0.000000001)
            local millisToFull = math.ceil(
                (capacity - tokens) / tokensPerMillisecond)
            local resetAt = now + millisToFull
            local retryAt = now

            if allowed == 0 then
                retryAt = now + math.ceil(
                    (1 - tokens) / tokensPerMillisecond)
            end

            redis.call(
                'HSET',
                KEYS[2],
                'tokens',
                tostring(tokens),
                'lastRefillAt',
                tostring(lastRefillAt))

            redis.call(
                'PEXPIRE',
                KEYS[2],
                math.max(1, millisToFull))

            redis.call('ZADD', KEYS[1], resetAt, member)

            return result(allowed, remaining, resetAt, retryAt)
            """;

    private static final RedisScript<String> TOKEN_BUCKET_SCRIPT = new DefaultRedisScript<>(SCRIPT_SOURCE,
            String.class);

    private final StringRedisTemplate redisTemplate;
    private final String keyPrefix;

    @Autowired
    RedisRateLimitStore(StringRedisTemplate redisTemplate) {
        this(redisTemplate, DEFAULT_KEY_PREFIX);
    }

    RedisRateLimitStore(
            StringRedisTemplate redisTemplate,
            String keyPrefix) {

        this.redisTemplate = Objects.requireNonNull(redisTemplate);
        this.keyPrefix = Objects.requireNonNull(keyPrefix);

        if (!keyPrefix.matches("[A-Za-z0-9:_-]+")) {
            throw new IllegalArgumentException(
                    "El prefijo de claves Redis no tiene un formato válido");
        }
    }

    RateLimitDecision consume(
            String clientFingerprint,
            RateLimitProperties properties) {

        Objects.requireNonNull(
                clientFingerprint,
                "La huella del cliente no puede ser nula");
        Objects.requireNonNull(properties);

        if (!clientFingerprint.matches(CLIENT_FINGERPRINT_PATTERN)) {
            throw new IllegalArgumentException(
                    "La huella del cliente no tiene un formato válido");
        }

        Duration window = properties.window();
        long windowMillis = window.toMillis();

        if (windowMillis < 1) {
            throw new IllegalArgumentException(
                    "La ventana del rate limiter debe ser de al menos un milisegundo");
        }

        String registryKey = keyPrefix + ":{global}:clients";
        String bucketKey = keyPrefix + ":{global}:bucket:" + clientFingerprint;

        String scriptResult = redisTemplate.execute(
                TOKEN_BUCKET_SCRIPT,
                List.of(registryKey, bucketKey),
                Integer.toString(properties.maxRequests()),
                Long.toString(windowMillis),
                Integer.toString(properties.maxTrackedClients()),
                clientFingerprint);

        return parseDecision(scriptResult);
    }

    private RateLimitDecision parseDecision(String result) {
        if (result == null) {
            throw new IllegalStateException(
                    "Redis no devolvió el resultado del rate limiter");
        }

        String[] values = result.split(":", -1);

        if (values.length != 4) {
            throw new IllegalStateException(
                    "Redis devolvió un resultado inválido para el rate limiter");
        }

        return new RateLimitDecision(
                "1".equals(values[0]),
                Instant.ofEpochMilli(Long.parseLong(values[2])),
                Instant.ofEpochMilli(Long.parseLong(values[3])),
                Integer.parseInt(values[1]));
    }
}