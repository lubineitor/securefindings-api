package com.securefindings.organization.api;

import com.securefindings.organization.domain.OrganizationStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateOrganizationStatusRequest(
        @NotNull(message = "El estado de la organización es obligatorio") OrganizationStatus status) {
}