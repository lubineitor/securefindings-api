package com.securefindings.audit.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

import com.securefindings.finding.domain.FindingStatus;

public record AuditEvent(
                UUID findingId,
                UUID organizationId,
                AuditAction action,
                String actor,
                Instant occurredAt,
                String requestId,
                FindingStatus previousStatus,
                FindingStatus newStatus) {

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

                if (action == AuditAction.STATUS_CHANGED) {
                        Objects.requireNonNull(
                                        previousStatus,
                                        "El estado anterior es obligatorio para una transición");
                        Objects.requireNonNull(
                                        newStatus,
                                        "El estado nuevo es obligatorio para una transición");

                        if (previousStatus == newStatus
                                        || !previousStatus.canTransitionTo(newStatus)) {
                                throw new IllegalArgumentException(
                                                "La transición de estado no es válida");
                        }
                } else if (previousStatus != null || newStatus != null) {
                        throw new IllegalArgumentException(
                                        "Solo una transición de estado puede incluir estados");
                }
        }

        public AuditEvent(
                        UUID findingId,
                        UUID organizationId,
                        AuditAction action,
                        String actor,
                        Instant occurredAt,
                        String requestId) {
                this(
                                findingId,
                                organizationId,
                                action,
                                actor,
                                occurredAt,
                                requestId,
                                null,
                                null);
        }
}