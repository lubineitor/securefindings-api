package com.securefindings.organization.api;

import com.securefindings.organization.api.validation.CodePointSize;
import com.securefindings.organization.domain.OrganizationStatus;
import com.securefindings.organization.domain.OrganizationStatusAuditEvent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateOrganizationStatusRequest(
                @NotNull(message = "El estado de la organización es obligatorio") OrganizationStatus status,

                @NotBlank(message = "El motivo del cambio es obligatorio") @CodePointSize(min = OrganizationStatusAuditEvent.MIN_REASON_LENGTH, max = OrganizationStatusAuditEvent.MAX_REASON_LENGTH) String reason) {

        public UpdateOrganizationStatusRequest {
                if (reason != null) {
                        reason = reason.strip();
                }
        }
}