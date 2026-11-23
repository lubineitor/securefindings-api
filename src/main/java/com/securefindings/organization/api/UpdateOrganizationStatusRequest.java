package com.securefindings.organization.api;

import com.securefindings.organization.domain.OrganizationStatus;
import com.securefindings.organization.domain.OrganizationStatusAuditEvent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateOrganizationStatusRequest(
                @NotNull(message = "El estado de la organización es obligatorio") OrganizationStatus status,

                @NotBlank(message = "El motivo del cambio es obligatorio") @Size(min = OrganizationStatusAuditEvent.MIN_REASON_LENGTH, max = OrganizationStatusAuditEvent.MAX_REASON_LENGTH, message = "El motivo debe tener entre 10 y 500 caracteres") String reason) {

        public UpdateOrganizationStatusRequest {
                if (reason != null) {
                        reason = reason.strip();
                }
        }
}