package com.securefindings.finding.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FindingStatusTest {

    @Test
    void deberiaPermitirTransicionesDesdeUnHallazgoAbierto() {
        assertTrue(FindingStatus.OPEN
                .canTransitionTo(FindingStatus.IN_PROGRESS));

        assertTrue(FindingStatus.OPEN
                .canTransitionTo(FindingStatus.RESOLVED));

        assertTrue(FindingStatus.OPEN
                .canTransitionTo(FindingStatus.FALSE_POSITIVE));
    }

    @Test
    void deberiaPermitirReabrirUnHallazgoFinalizado() {
        assertTrue(FindingStatus.RESOLVED
                .canTransitionTo(FindingStatus.OPEN));

        assertTrue(FindingStatus.FALSE_POSITIVE
                .canTransitionTo(FindingStatus.OPEN));
    }

    @Test
    void deberiaRechazarCambiosEntreEstadosFinalizados() {
        assertFalse(FindingStatus.RESOLVED
                .canTransitionTo(FindingStatus.FALSE_POSITIVE));

        assertFalse(FindingStatus.FALSE_POSITIVE
                .canTransitionTo(FindingStatus.RESOLVED));
    }

    @Test
    void deberiaPermitirMantenerElMismoEstado() {
        assertTrue(FindingStatus.OPEN
                .canTransitionTo(FindingStatus.OPEN));

        assertTrue(FindingStatus.RESOLVED
                .canTransitionTo(FindingStatus.RESOLVED));
    }
}