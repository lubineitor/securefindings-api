package com.securefindings.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

class RedisRateLimitStoreConcurrencyIntegrationTest {

    @Test
    void noDebeSuperarLaCapacidadConPeticionesConcurrentes()
            throws Exception {

        int capacity = 25;
        int totalRequests = 75;
        int workerCount = 15;

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
                    "rate-limit-concurrency-test");

            RateLimitProperties properties = new RateLimitProperties(
                    capacity,
                    Duration.ofHours(1),
                    100);

            String client = RateLimitFilter.fingerprintClientKey(
                    "concurrent-test-client");

            ExecutorService executor = Executors.newFixedThreadPool(workerCount);
            CountDownLatch ready = new CountDownLatch(workerCount);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Boolean>> results = new ArrayList<>(totalRequests);

            try {
                for (int i = 0; i < totalRequests; i++) {
                    results.add(executor.submit(() -> {
                        ready.countDown();

                        if (!start.await(10, TimeUnit.SECONDS)) {
                            throw new IllegalStateException(
                                    "No se inició la prueba concurrente");
                        }

                        return store.consume(client, properties).allowed();
                    }));
                }

                assertTrue(
                        ready.await(10, TimeUnit.SECONDS),
                        "Los workers no quedaron preparados a tiempo");

                start.countDown();

                int allowedRequests = 0;

                for (Future<Boolean> result : results) {
                    if (result.get(20, TimeUnit.SECONDS)) {
                        allowedRequests++;
                    }
                }

                assertEquals(
                        capacity,
                        allowedRequests,
                        "Redis debe permitir exactamente las peticiones de la capacidad");
            } finally {
                start.countDown();
                executor.shutdownNow();
            }
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
}