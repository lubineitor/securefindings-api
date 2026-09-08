package com.securefindings.comment.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class FindingCommentTest {

    private static final UUID FINDING_ID = UUID.randomUUID();

    private static final UUID ORGANIZATION_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000001");

    @Test
    void deberiaCrearUnComentarioValido() {
        FindingComment comment = FindingComment.create(
                FINDING_ID,
                ORGANIZATION_ID,
                "analista",
                "Se ha revisado el hallazgo.");

        assertNotNull(comment.id());
        assertEquals(FINDING_ID, comment.findingId());
        assertEquals(ORGANIZATION_ID, comment.organizationId());
        assertEquals("analista", comment.author());
        assertEquals(
                "Se ha revisado el hallazgo.",
                comment.content());
        assertNotNull(comment.createdAt());
    }

    @Test
    void deberiaRechazarUnAutorVacio() {
        assertThrows(
                IllegalArgumentException.class,
                () -> FindingComment.create(
                        FINDING_ID,
                        ORGANIZATION_ID,
                        " ",
                        "Contenido válido"));
    }

    @Test
    void deberiaRechazarUnContenidoVacio() {
        assertThrows(
                IllegalArgumentException.class,
                () -> FindingComment.create(
                        FINDING_ID,
                        ORGANIZATION_ID,
                        "analista",
                        " "));
    }

    @Test
    void deberiaRechazarUnContenidoDemasiadoLargo() {
        String content = "a".repeat(
                FindingComment.MAX_CONTENT_LENGTH + 1);

        assertThrows(
                IllegalArgumentException.class,
                () -> FindingComment.create(
                        FINDING_ID,
                        ORGANIZATION_ID,
                        "analista",
                        content));
    }

    @Test
    void deberiaAceptarElContenidoConLaLongitudMaxima() {
        String content = "a".repeat(
                FindingComment.MAX_CONTENT_LENGTH);

        FindingComment comment = FindingComment.create(
                FINDING_ID,
                ORGANIZATION_ID,
                "analista",
                content);

        assertEquals(
                FindingComment.MAX_CONTENT_LENGTH,
                comment.content().length());
    }
}