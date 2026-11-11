package com.securefindings.finding.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class FindingTest {

        @Test
        void deberiaCrearUnHallazgoAbiertoSinFechaLimite() {
                Finding finding = Finding.create(
                                "SQL Injection",
                                "Entrada de usuario sin validar",
                                FindingSeverity.HIGH);

                assertNotNull(finding.id());
                assertEquals("SQL Injection", finding.title());
                assertEquals(FindingSeverity.HIGH, finding.severity());
                assertEquals(FindingStatus.OPEN, finding.status());
                assertNull(finding.dueAt());
                assertNotNull(finding.createdAt());
                assertEquals(finding.createdAt(), finding.updatedAt());
        }

        @Test
        void deberiaAsignarYQuitarLaFechaLimite() {
                Instant dueAt = Instant.parse("2026-12-01T09:00:00Z");
                UUID assetId = UUID.fromString(
                                "22222222-2222-2222-2222-222222222222");

                Finding finding = Finding.create(
                                "SQL Injection",
                                "Entrada de usuario sin validar",
                                FindingSeverity.HIGH,
                                assetId,
                                dueAt);

                assertEquals(dueAt, finding.dueAt());
                assertEquals(assetId, finding.assetId());
                assertEquals(dueAt, finding.withStatus(FindingStatus.IN_PROGRESS).dueAt());
                assertEquals(dueAt, finding.withDetails(
                                "Título actualizado",
                                "Descripción actualizada",
                                FindingSeverity.CRITICAL).dueAt());
                assertEquals(dueAt, finding.withAssetId(null).dueAt());
                assertNull(finding.withDueAt(null).dueAt());
        }

        @Test
        void noDeberiaPermitirUnTituloVacio() {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> Finding.create(
                                                " ",
                                                "Descripción válida",
                                                FindingSeverity.MEDIUM));
        }

        @Test
        void noDeberiaPermitirUnaDescripcionVacia() {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> Finding.create(
                                                "Título válido",
                                                "",
                                                FindingSeverity.MEDIUM));
        }
}