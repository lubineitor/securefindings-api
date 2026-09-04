package com.securefindings.organization.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Organization(
        UUID id,
        String name,
        String slug,
        Instant createdAt) {

    public Organization {
        Objects.requireNonNull(id, "El identificador no puede ser nulo");
        Objects.requireNonNull(createdAt, "La fecha de creación no puede ser nula");

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre de la organización no puede estar vacío");
        }

        if (slug == null
                || !slug.matches("[a-z0-9]+(?:-[a-z0-9]+)*")) {
            throw new IllegalArgumentException(
                    "El slug de la organización no tiene un formato válido");
        }
    }
}