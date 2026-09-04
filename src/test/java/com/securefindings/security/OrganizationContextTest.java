package com.securefindings.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.securefindings.organization.persistence.OrganizationRepository;

@ExtendWith(MockitoExtension.class)
class OrganizationContextTest {

    private static final UUID ORGANIZATION_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000001");

    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private OrganizationContext organizationContext;

    @AfterEach
    void limpiarContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deberiaObtenerLaOrganizacionDesdeElToken() {
        when(organizationRepository.existsById(ORGANIZATION_ID))
                .thenReturn(true);

        autenticarConClaim(
                ORGANIZATION_ID.toString());

        UUID organizationId = organizationContext.currentOrganizationId();

        assertEquals(ORGANIZATION_ID, organizationId);

        verify(organizationRepository)
                .existsById(ORGANIZATION_ID);
    }

    @Test
    void deberiaRechazarUnTokenSinOrganizacion() {
        autenticarSinClaimDeOrganizacion();

        assertThrows(
                AccessDeniedException.class,
                () -> organizationContext.currentOrganizationId());

        verifyNoInteractions(organizationRepository);
    }

    @Test
    void deberiaRechazarUnClaimConFormatoInvalido() {
        autenticarConClaim("organizacion-invalida");

        assertThrows(
                AccessDeniedException.class,
                () -> organizationContext.currentOrganizationId());

        verifyNoInteractions(organizationRepository);
    }

    @Test
    void deberiaRechazarUnaOrganizacionInexistente() {
        UUID unknownOrganizationId = UUID.randomUUID();

        when(organizationRepository
                .existsById(unknownOrganizationId))
                .thenReturn(false);

        autenticarConClaim(
                unknownOrganizationId.toString());

        assertThrows(
                AccessDeniedException.class,
                () -> organizationContext.currentOrganizationId());

        verify(organizationRepository)
                .existsById(unknownOrganizationId);
    }

    @Test
    void deberiaUsarLaOrganizacionDelSistemaSinAutenticacion() {
        SecurityContextHolder.clearContext();

        UUID organizationId = organizationContext.currentOrganizationId();

        assertEquals(
                ORGANIZATION_ID,
                organizationId);

        verifyNoInteractions(organizationRepository);
    }

    private void autenticarConClaim(String organizationId) {
        Jwt jwt = Jwt.withTokenValue("token-de-prueba")
                .header("alg", "RS256")
                .claim("organization_id", organizationId)
                .issuedAt(Instant.now())
                .expiresAt(
                        Instant.now().plusSeconds(300))
                .build();

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);

        authentication.setAuthenticated(true);

        establecerAutenticacion(authentication);
    }

    private void autenticarSinClaimDeOrganizacion() {
        Jwt jwt = Jwt.withTokenValue("token-de-prueba")
                .header("alg", "RS256")
                .claim("preferred_username", "analista")
                .issuedAt(Instant.now())
                .expiresAt(
                        Instant.now().plusSeconds(300))
                .build();

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);

        authentication.setAuthenticated(true);

        establecerAutenticacion(authentication);
    }

    private void establecerAutenticacion(
            Authentication authentication) {

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(authentication);

        SecurityContextHolder.setContext(securityContext);
    }
}