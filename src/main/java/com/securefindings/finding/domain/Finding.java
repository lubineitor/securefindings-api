package com.securefindings.finding.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Finding(
        UUID id,
        String title,
        String description,
        FindingSeverity severity,
        FindingStatus status,
        Instant createdAt,
        Instant updatedAt,
        UUID assetId) {

    public Finding(
            UUID id,
            String title,
            String description,
            FindingSeverity severity,
            FindingStatus status,
            Instant createdAt,
            Instant updatedAt) {
        this(id, title, description, severity, status, createdAt, updatedAt, null);
    }

    public Finding {
        Objects.requireNonNull(id, "El identificador no puede ser nulo");
        Objects.requireNonNull(severity, "La severidad no puede ser nula");
        Objects.requireNonNull(status, "El estado no puede ser nulo");
        Objects.requireNonNull(createdAt, "La fecha de creación no puede ser nula");
        Objects.requireNonNull(updatedAt, "La fecha de actualización no puede ser nula");

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("El título no puede estar vacío");
        }

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("La descripción no puede estar vacía");
        }
    }

    public static Finding create(
            String title,
            String description,
            FindingSeverity severity) {
        return create(title, description, severity, null);
    }

    public static Finding create(
            String title,
            String description,
            FindingSeverity severity,
            UUID assetId) {
        Instant now = Instant.now();

        return new Finding(
                UUID.randomUUID(),
                title,
                description,
                severity,
                FindingStatus.OPEN,
                now,
                now,
                assetId);
    }

    public Finding withStatus(FindingStatus newStatus) {
        return new Finding(
                id,
                title,
                description,
                severity,
                newStatus,
                createdAt,
                Instant.now(),
                assetId);
    }

    public Finding withDetails(
            String newTitle,
            String newDescription,
            FindingSeverity newSeverity) {
        return new Finding(
                id,
                newTitle,
                newDescription,
                newSeverity,
                status,
                createdAt,
                Instant.now(),
                assetId);
    }

    public Finding withAssetId(UUID newAssetId) {
        return new Finding(
                id,
                title,
                description,
                severity,
                status,
                createdAt,
                Instant.now(),
                newAssetId);
    }
}