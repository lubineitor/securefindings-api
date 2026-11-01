package com.securefindings.asset.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AssetAuditEvent(
        UUID id,
        UUID assetId,
        UUID organizationId,
        AssetAuditAction action,
        String actor,
        Instant occurredAt,
        String requestId) {

    public AssetAuditEvent {
        Objects.requireNonNull(id, "El identificador del evento no puede ser nulo");
        Objects.requireNonNull(assetId, "El identificador del activo no puede ser nulo");
        Objects.requireNonNull(organizationId, "La organización no puede ser nula");
        Objects.requireNonNull(action, "La acción no puede ser nula");
        Objects.requireNonNull(occurredAt, "La fecha del evento no puede ser nula");

        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("El actor no puede estar vacío");
        }

        actor = actor.trim();
    }
}