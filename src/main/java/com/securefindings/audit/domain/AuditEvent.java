package com.securefindings.audit.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AuditEvent(
                UUID findingId,
                UUID organizationId,
                AuditAction action,
                String actor,
                Instant occurredAt) {

        public AuditEvent {
                Objects.requireNonNull(
                                findingId,
                                "El identificador del hallazgo no puede ser nulo");

                Objects.requireNonNull(
                                organizationId,
                                "El identificador de la organización no puede ser nulo");

                Objects.requireNonNull(
                                action,
                                "La acción no puede ser nula");

                Objects.requireNonNull(
                                actor,
                                "El actor no puede ser nulo");

                Objects.requireNonNull(
                                occurredAt,
                                "La fecha del evento no puede ser nula");
        }
}