package com.securefindings.comment.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record FindingComment(
        UUID id,
        UUID findingId,
        UUID organizationId,
        String author,
        String content,
        Instant createdAt) {

    public static final int MAX_CONTENT_LENGTH = 5000;

    public FindingComment {
        Objects.requireNonNull(
                id,
                "El identificador del comentario no puede ser nulo");

        Objects.requireNonNull(
                findingId,
                "El identificador del hallazgo no puede ser nulo");

        Objects.requireNonNull(
                organizationId,
                "El identificador de la organización no puede ser nulo");

        Objects.requireNonNull(
                createdAt,
                "La fecha de creación no puede ser nula");

        if (author == null || author.isBlank()) {
            throw new IllegalArgumentException(
                    "El autor no puede estar vacío");
        }

        if (author.length() > 255) {
            throw new IllegalArgumentException(
                    "El autor no puede superar los 255 caracteres");
        }

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException(
                    "El contenido no puede estar vacío");
        }

        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException(
                    "El contenido no puede superar los 5000 caracteres");
        }
    }

    public static FindingComment create(
            UUID findingId,
            UUID organizationId,
            String author,
            String content) {

        return new FindingComment(
                UUID.randomUUID(),
                findingId,
                organizationId,
                author,
                content,
                Instant.now());
    }
}