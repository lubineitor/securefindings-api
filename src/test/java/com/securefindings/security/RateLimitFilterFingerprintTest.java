package com.securefindings.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RateLimitFilterFingerprintTest {

    @Test
    void deberiaGenerarUnaHuellaDeLongitudFijaParaUnaClaveLarga() {
        String clientKey = "organization:00000000-0000-0000-0000-000000000001"
                + ":issuer:https://identity.example"
                + ":subject:"
                + "usuario".repeat(1_000);

        String fingerprint = RateLimitFilter.fingerprintClientKey(clientKey);

        assertEquals(64, fingerprint.length());
        assertTrue(fingerprint.matches("[0-9a-f]{64}"));
        assertNotEquals(clientKey, fingerprint);
    }

    @Test
    void deberiaGenerarLaMismaHuellaParaLaMismaClave() {
        String clientKey = "user:analista";

        assertEquals(
                RateLimitFilter.fingerprintClientKey(clientKey),
                RateLimitFilter.fingerprintClientKey(clientKey));
    }

    @Test
    void deberiaGenerarHuellasDiferentesParaClavesDiferentes() {
        String firstFingerprint = RateLimitFilter
                .fingerprintClientKey("ip:10.0.0.1");

        String secondFingerprint = RateLimitFilter
                .fingerprintClientKey("ip:10.0.0.2");

        assertNotEquals(firstFingerprint, secondFingerprint);
    }
}