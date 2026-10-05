package com.securefindings.organization.api;

import java.time.Instant;
import java.util.UUID;

import com.securefindings.organization.domain.Organization;

public record OrganizationResponse(
        UUID id,
        String name,
        String slug,
        Instant createdAt) {

    public static OrganizationResponse from(Organization organization) {
        return new OrganizationResponse(
                organization.id(),
                organization.name(),
                organization.slug(),
                organization.createdAt());
    }
}