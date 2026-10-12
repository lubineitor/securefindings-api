package com.securefindings.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtAudienceValidator;

class JwtAudienceValidatorTest {

    private static final Instant ISSUED_AT = Instant.parse("2026-10-12T00:00:00Z");

    private static final Instant EXPIRES_AT = Instant.parse("2026-10-12T00:05:00Z");

    private final JwtAudienceValidator validator = new JwtAudienceValidator("securefindings-api");

    @Test
    void deberiaAceptarLaAudienciaEsperadaAunqueExistaOtraAdicional() {
        Jwt jwt = createJwt(List.of("account", "securefindings-api"));

        assertFalse(validator.validate(jwt).hasErrors());
    }

    @Test
    void deberiaRechazarUnTokenQueSoloTieneOtraAudiencia() {
        Jwt jwt = createJwt(List.of("account"));

        assertTrue(validator.validate(jwt).hasErrors());
    }

    private Jwt createJwt(List<String> audiences) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .issuedAt(ISSUED_AT)
                .expiresAt(EXPIRES_AT)
                .claim("aud", audiences)
                .build();
    }
}