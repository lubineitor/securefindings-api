package com.securefindings.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class RateLimitPropertiesTest {

    @Test
    void deberiaRechazarUnaVentanaInferiorAUnMilisegundo() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new RateLimitProperties(
                        10,
                        Duration.ofNanos(999_999),
                        100));

        assertTrue(exception.getMessage()
                .contains("al menos un milisegundo"));
    }

    @Test
    void deberiaAceptarUnaVentanaDeUnMilisegundo() {
        RateLimitProperties properties = new RateLimitProperties(
                10,
                Duration.ofMillis(1),
                100);

        assertEquals(Duration.ofMillis(1), properties.window());
    }

    @Test
    void deberiaRechazarUnaVentanaQueDesbordaAlConvertirAMilisegundos() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new RateLimitProperties(
                        10,
                        Duration.ofSeconds(Long.MAX_VALUE),
                        100));

        assertTrue(exception.getMessage()
                .contains("demasiado grande"));
    }
}