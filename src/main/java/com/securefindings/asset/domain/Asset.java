package com.securefindings.asset.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Asset(
        UUID id,
        String name,
        String identifier,
        AssetType type,
        AssetEnvironment environment,
        AssetCriticality criticality,
        AssetStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public Asset {
        Objects.requireNonNull(id, "El identificador del activo no puede ser nulo");
        Objects.requireNonNull(type, "El tipo del activo no puede ser nulo");
        Objects.requireNonNull(environment, "El entorno del activo no puede ser nulo");
        Objects.requireNonNull(criticality, "La criticidad del activo no puede ser nula");
        Objects.requireNonNull(status, "El estado del activo no puede ser nulo");
        Objects.requireNonNull(createdAt, "La fecha de creación no puede ser nula");
        Objects.requireNonNull(updatedAt, "La fecha de actualización no puede ser nula");

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre del activo no puede estar vacío");
        }
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("El identificador externo no puede estar vacío");
        }

        name = name.trim();
        identifier = identifier.trim();
    }

    public static Asset create(
            String name,
            String identifier,
            AssetType type,
            AssetEnvironment environment,
            AssetCriticality criticality) {
        Instant now = Instant.now();
        return new Asset(
                UUID.randomUUID(),
                name,
                identifier,
                type,
                environment,
                criticality,
                AssetStatus.ACTIVE,
                now,
                now);
    }

    public Asset withDetails(
            String newName,
            String newIdentifier,
            AssetType newType,
            AssetEnvironment newEnvironment,
            AssetCriticality newCriticality,
            AssetStatus newStatus) {
        return new Asset(
                id,
                newName,
                newIdentifier,
                newType,
                newEnvironment,
                newCriticality,
                newStatus,
                createdAt,
                Instant.now());
    }
}