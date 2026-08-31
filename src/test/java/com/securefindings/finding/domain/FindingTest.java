package com.securefindings.finding.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FindingTest {

    @Test
    void deberiaCrearUnHallazgoAbierto() {
        Finding finding = Finding.create(
                "SQL Injection",
                "Entrada de usuario sin validar",
                FindingSeverity.HIGH
        );

        assertNotNull(finding.id());
        assertEquals("SQL Injection", finding.title());
        assertEquals(FindingSeverity.HIGH, finding.severity());
        assertEquals(FindingStatus.OPEN, finding.status());
        assertNotNull(finding.createdAt());
        assertEquals(finding.createdAt(), finding.updatedAt());
    }

    @Test
    void noDeberiaPermitirUnTituloVacio() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Finding.create(
                        " ",
                        "Descripción válida",
                        FindingSeverity.MEDIUM
                )
        );
    }

    @Test
    void noDeberiaPermitirUnaDescripcionVacia() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Finding.create(
                        "Título válido",
                        "",
                        FindingSeverity.MEDIUM
                )
        );
    }
}