package com.securefindings.audit.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public record AuditEvent(
                UUID findingId,
                UUID organizationId,
                AuditAction action,
                String actor,
                Instant occurredAt,
                String requestId) {

        private static final Pattern VALID_REQUEST_ID = Pattern.compile(
                        "[A-Za-z0-9][A-Za-z0-9._-]{0,63}");

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

                if (requestId != null
                                && !VALID_REQUEST_ID.matcher(requestId).matches()) {
                        throw new IllegalArgumentException(
                                        "El identificador de petición no tiene un formato válido");
                }
        }
}