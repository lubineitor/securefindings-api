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
        UUID assetId,
        Instant dueAt) {

    public Finding(
            UUID id,
            String title,
            String description,
            FindingSeverity severity,
            FindingStatus status,
            Instant createdAt,
            Instant updatedAt) {
        this(
                id,
                title,
                description,
                severity,
                status,
                createdAt,
                updatedAt,
                null,
                null);
    }

    public Finding(
            UUID id,
            String title,
            String description,
            FindingSeverity severity,
            FindingStatus status,
            Instant createdAt,
            Instant updatedAt,
            UUID assetId) {
        this(
                id,
                title,
                description,
                severity,
                status,
                createdAt,
                updatedAt,
                assetId,
                null);
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
        return create(title, description, severity, null, null);
    }

    public static Finding create(
            String title,
            String description,
            FindingSeverity severity,
            UUID assetId) {
        return create(title, description, severity, assetId, null);
    }

    public static Finding create(
            String title,
            String description,
            FindingSeverity severity,
            UUID assetId,
            Instant dueAt) {
        Instant now = Instant.now();

        return new Finding(
                UUID.randomUUID(),
                title,
                description,
                severity,
                FindingStatus.OPEN,
                now,
                now,
                assetId,
                dueAt);
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
                assetId,
                dueAt);
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
                assetId,
                dueAt);
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
                newAssetId,
                dueAt);
    }

    public Finding withDueAt(Instant newDueAt) {
        return new Finding(
                id,
                title,
                description,
                severity,
                status,
                createdAt,
                Instant.now(),
                assetId,
                newDueAt);
    }
}