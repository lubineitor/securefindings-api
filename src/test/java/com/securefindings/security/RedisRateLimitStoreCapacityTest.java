package com.securefindings.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

class RedisRateLimitStoreCapacityTest {

    @Test
    void deberiaLiberarEspacioParaOtroClienteAlExpirarLaCuota()
            throws Exception {

        withRedisStore(store -> {
            RateLimitProperties properties = new RateLimitProperties(
                    1,
                    Duration.ofSeconds(2),
                    1);

            String firstClient = RateLimitFilter.fingerprintClientKey("test-client-1");
            String secondClient = RateLimitFilter.fingerprintClientKey("test-client-2");

            assertTrue(store.consume(firstClient, properties).allowed());
            assertFalse(store.consume(secondClient, properties).allowed());

            Thread.sleep(properties.window().toMillis() + 200);

            assertTrue(store.consume(secondClient, properties).allowed());
        });
    }

    @Test
    void deberiaReponerTokensGradualmenteAntesDeCompletarLaVentana()
            throws Exception {

        withRedisStore(store -> {
            RateLimitProperties properties = new RateLimitProperties(
                    2,
                    Duration.ofSeconds(4),
                    1);

            String client = RateLimitFilter.fingerprintClientKey("test-client-refill");

            assertTrue(store.consume(client, properties).allowed());
            assertTrue(store.consume(client, properties).allowed());
            assertFalse(store.consume(client, properties).allowed());

            Thread.sleep(2_200);

            assertTrue(store.consume(client, properties).allowed());
            assertFalse(store.consume(client, properties).allowed());
        });
    }

    private void withRedisStore(StoreTest test) throws Exception {
        GenericContainer<?> redis = new GenericContainer<>(
                DockerImageName.parse("redis:7-alpine"));
        redis.withExposedPorts(6379);

        LettuceConnectionFactory connectionFactory = null;

        try {
            redis.start();

            connectionFactory = new LettuceConnectionFactory(
                    redis.getHost(),
                    redis.getMappedPort(6379));
            connectionFactory.afterPropertiesSet();

            StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);
            redisTemplate.afterPropertiesSet();

            RedisRateLimitStore store = new RedisRateLimitStore(
                    redisTemplate,
                    "rate-limit-capacity-test");

            test.run(store);
        } finally {
            try {
                if (connectionFactory != null) {
                    connectionFactory.destroy();
                }
            } finally {
                redis.close();
            }
        }
    }

    @FunctionalInterface
    private interface StoreTest {
        void run(RedisRateLimitStore store) throws Exception;
    }
}