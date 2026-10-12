package com.securefindings.security;

import java.time.Duration;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "securefindings.rate-limit")
public record RateLimitProperties(
        int maxRequests,
        Duration window,
        int maxTrackedClients) {

    public RateLimitProperties {
        if (maxRequests < 1) {
            throw new IllegalArgumentException(
                    "El número máximo de peticiones debe ser mayor que cero");
        }

        Objects.requireNonNull(
                window,
                "La ventana de limitación no puede ser nula");

        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException(
                    "La ventana de limitación debe ser positiva");
        }

        long windowMillis;

        try {
            windowMillis = window.toMillis();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "La ventana de limitación es demasiado grande",
                    exception);
        }

        if (windowMillis < 1) {
            throw new IllegalArgumentException(
                    "La ventana de limitación debe ser de al menos un milisegundo");
        }

        if (maxTrackedClients < 1) {
            throw new IllegalArgumentException(
                    "El máximo de clientes registrados debe ser mayor que cero");
        }
    }
}