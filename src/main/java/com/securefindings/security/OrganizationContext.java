package com.securefindings.security;

import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.securefindings.organization.persistence.OrganizationRepository;

@Component
public class OrganizationContext {

    private static final String ORGANIZATION_CLAIM = "organization_id";

    private static final UUID SYSTEM_ORGANIZATION_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000001");

    private final OrganizationRepository organizationRepository;

    public OrganizationContext(
            OrganizationRepository organizationRepository) {

        this.organizationRepository = organizationRepository;
    }

    public UUID currentOrganizationId() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {
            return SYSTEM_ORGANIZATION_ID;
        }

        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            throw new AccessDeniedException(
                    "La autenticación no contiene un JWT válido");
        }

        String rawOrganizationId = jwtAuthentication
                .getToken()
                .getClaimAsString(ORGANIZATION_CLAIM);

        if (rawOrganizationId == null
                || rawOrganizationId.isBlank()) {
            throw new AccessDeniedException(
                    "El token no contiene el claim organization_id");
        }

        UUID organizationId;

        try {
            organizationId = UUID.fromString(rawOrganizationId);
        } catch (IllegalArgumentException exception) {
            throw new AccessDeniedException(
                    "El claim organization_id no contiene un UUID válido",
                    exception);
        }

        if (!organizationRepository.existsById(organizationId)) {
            throw new AccessDeniedException(
                    "La organización del token no existe");
        }

        return organizationId;
    }
}