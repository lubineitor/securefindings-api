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
        String requestId,
        String reason) {

    public static final int MIN_REASON_LENGTH = 10;
    public static final int MAX_REASON_LENGTH = 500;

    /**
     * Compatibilidad para construir eventos históricos que no tienen motivo.
     */
    public OrganizationStatusAuditEvent(
            UUID id,
            UUID organizationId,
            OrganizationStatusAuditAction action,
            String actor,
            Instant occurredAt,
            String requestId) {
        this(id, organizationId, action, actor, occurredAt, requestId, null);
    }

    public OrganizationStatusAuditEvent {
        Objects.requireNonNull(id, "El identificador del evento no puede ser nulo");
        Objects.requireNonNull(organizationId, "La organización no puede ser nula");
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

        if (reason != null) {
            reason = reason.strip();

            if (reason.length() < MIN_REASON_LENGTH
                    || reason.length() > MAX_REASON_LENGTH) {
                throw new IllegalArgumentException(
                        "El motivo debe tener entre 10 y 500 caracteres");
            }
        }
    }
}