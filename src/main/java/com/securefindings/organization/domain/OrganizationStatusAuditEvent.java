package com.securefindings.organization.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record OrganizationStatusAuditEvent(
        UUID id,
        UUID organizationId,
        OrganizationStatusAuditAction action,
        String actor,
        Instant occurredAt,
        String requestId) {

    public OrganizationStatusAuditEvent {
        Objects.requireNonNull(id, "El identificador del evento no puede ser nulo");
        Objects.requireNonNull(
                organizationId,
                "La organización no puede ser nula");
        Objects.requireNonNull(action, "La acción no puede ser nula");
        Objects.requireNonNull(occurredAt, "La fecha del evento no puede ser nula");

        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("El actor no puede estar vacío");
        }

        actor = actor.trim();

        if (requestId != null && requestId.length() > 64) {
            throw new IllegalArgumentException(
                    "El identificador de petición no puede superar 64 caracteres");
        }
    }
}