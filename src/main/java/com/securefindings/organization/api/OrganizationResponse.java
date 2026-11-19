package com.securefindings.organization.api;

import java.time.Instant;
import java.util.UUID;

import com.securefindings.organization.domain.Organization;
import com.securefindings.organization.domain.OrganizationStatus;

public record OrganizationResponse(
        UUID id,
        String name,
        String slug,
        Instant createdAt,
        OrganizationStatus status) {

    public OrganizationResponse(
            UUID id,
            String name,
            String slug,
            Instant createdAt) {
        this(id, name, slug, createdAt, OrganizationStatus.ACTIVE);
    }

    public static OrganizationResponse from(Organization organization) {
        return new OrganizationResponse(
                organization.id(),
                organization.name(),
                organization.slug(),
                organization.createdAt(),
                organization.status());
    }
}