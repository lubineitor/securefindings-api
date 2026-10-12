package com.securefindings.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

class RedisRateLimitStoreDistributedIntegrationTest {

    @Test
    void deberiaCompartirLaCuotaEntreInstanciasIndependientes()
            throws Exception {

        GenericContainer<?> redis = new GenericContainer<>(
                DockerImageName.parse("redis:7-alpine"));
        redis.withExposedPorts(6379);

        LettuceConnectionFactory firstConnectionFactory = null;
        LettuceConnectionFactory secondConnectionFactory = null;

        try {
            redis.start();

            firstConnectionFactory = createConnectionFactory(redis);
            secondConnectionFactory = createConnectionFactory(redis);

            RedisRateLimitStore firstStore = new RedisRateLimitStore(
                    createRedisTemplate(firstConnectionFactory),
                    "rate-limit-distributed-test");

            RedisRateLimitStore secondStore = new RedisRateLimitStore(
                    createRedisTemplate(secondConnectionFactory),
                    "rate-limit-distributed-test");

            RateLimitProperties properties = new RateLimitProperties(
                    3,
                    Duration.ofHours(1),
                    100);

            String client = RateLimitFilter.fingerprintClientKey(
                    "shared-application-client");

            assertTrue(firstStore.consume(client, properties).allowed());
            assertTrue(secondStore.consume(client, properties).allowed());
            assertTrue(firstStore.consume(client, properties).allowed());
            assertFalse(secondStore.consume(client, properties).allowed());
        } finally {
            try {
                if (firstConnectionFactory != null) {
                    firstConnectionFactory.destroy();
                }
            } finally {
                try {
                    if (secondConnectionFactory != null) {
                        secondConnectionFactory.destroy();
                    }
                } finally {
                    redis.close();
                }
            }
        }
    }

    private LettuceConnectionFactory createConnectionFactory(
            GenericContainer<?> redis) {

        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(
                redis.getHost(),
                redis.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();

        return connectionFactory;
    }

    private StringRedisTemplate createRedisTemplate(
            LettuceConnectionFactory connectionFactory) {

        StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();

        return redisTemplate;
    }
}