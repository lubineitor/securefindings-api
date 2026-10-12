package com.securefindings.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class RedisRateLimitStoreTest {

    @SuppressWarnings("resource")
    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;

    @BeforeAll
    static void conectarRedis() {
        connectionFactory = new LettuceConnectionFactory(
                redis.getHost(),
                redis.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();

        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
    }

    @AfterAll
    static void cerrarRedis() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    void deberiaCompartirLaCuotaEntreInstanciasDelAlmacen() {
        String namespace = namespace();
        String fingerprint = "a".repeat(64);
        RateLimitProperties properties = properties(2, 100);

        RedisRateLimitStore firstInstance = new RedisRateLimitStore(
                redisTemplate,
                namespace);
        RedisRateLimitStore secondInstance = new RedisRateLimitStore(
                redisTemplate,
                namespace);

        RateLimitDecision first = firstInstance.consume(
                fingerprint,
                properties);
        RateLimitDecision second = secondInstance.consume(
                fingerprint,
                properties);
        RateLimitDecision third = firstInstance.consume(
                fingerprint,
                properties);

        assertTrue(first.allowed());
        assertEquals(1, first.remaining());

        assertTrue(second.allowed());
        assertEquals(0, second.remaining());

        assertFalse(third.allowed());
        assertEquals(0, third.remaining());
        assertTrue(third.retryAt().isAfter(third.resetAt().minusSeconds(61)));
    }

    @Test
    void deberiaRechazarClientesNuevosAlAlcanzarElMaximoRegistrado() {
        RedisRateLimitStore store = new RedisRateLimitStore(
                redisTemplate,
                namespace());

        RateLimitProperties properties = properties(1, 1);

        RateLimitDecision firstClient = store.consume(
                "b".repeat(64),
                properties);
        RateLimitDecision secondClient = store.consume(
                "c".repeat(64),
                properties);

        assertTrue(firstClient.allowed());
        assertFalse(secondClient.allowed());
        assertEquals(0, secondClient.remaining());
    }

    @Test
    void noDebeSuperarLaCuotaAnteConsumosConcurrentesDeVariasInstancias()
            throws Exception {

        int maxRequests = 5;
        int concurrentRequests = 32;
        String fingerprint = "d".repeat(64);
        RateLimitProperties properties = properties(maxRequests, 100);

        String namespace = namespace();
        RedisRateLimitStore firstInstance = new RedisRateLimitStore(
                redisTemplate,
                namespace);
        RedisRateLimitStore secondInstance = new RedisRateLimitStore(
                redisTemplate,
                namespace);

        CountDownLatch ready = new CountDownLatch(concurrentRequests);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>(concurrentRequests);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            for (int i = 0; i < concurrentRequests; i++) {
                RedisRateLimitStore store = i % 2 == 0
                        ? firstInstance
                        : secondInstance;

                results.add(executor.submit(() -> {
                    ready.countDown();

                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException(
                                "No se inició la prueba concurrente");
                    }

                    return store.consume(
                            fingerprint,
                            properties)
                            .allowed();
                }));
            }

            boolean allReady = ready.await(10, TimeUnit.SECONDS);
            start.countDown();

            assertTrue(
                    allReady,
                    "Todas las tareas deben estar preparadas");

            int allowedRequests = 0;

            for (Future<Boolean> result : results) {
                if (result.get(10, TimeUnit.SECONDS)) {
                    allowedRequests++;
                }
            }

            assertEquals(maxRequests, allowedRequests);
        }
    }

    private static RateLimitProperties properties(
            int maxRequests,
            int maxTrackedClients) {

        return new RateLimitProperties(
                maxRequests,
                Duration.ofMinutes(1),
                maxTrackedClients);
    }

    private static String namespace() {
        return "securefindings:test:" + UUID.randomUUID();
    }
}