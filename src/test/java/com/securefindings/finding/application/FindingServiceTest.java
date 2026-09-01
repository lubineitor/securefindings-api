package com.securefindings.finding.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.securefindings.finding.domain.Finding;
import com.securefindings.finding.domain.FindingSeverity;
import com.securefindings.finding.domain.FindingStatus;

class FindingServiceTest {

    private final FindingService findingService = new FindingService();

    @Test
    void deberiaCrearYListarUnHallazgo() {
        Finding finding = findingService.create(
                "SQL Injection",
                "Entrada de usuario sin validar",
                FindingSeverity.HIGH
        );

        List<Finding> findings = findingService.findAll();

        assertNotNull(finding);
        assertEquals(1, findings.size());
        assertEquals(finding, findings.get(0));
        assertEquals(FindingStatus.OPEN, finding.status());
    }

    @Test
    void deberiaEncontrarUnHallazgoPorSuIdentificador() {
        Finding createdFinding = findingService.create(
                "Cross-Site Scripting",
                "Contenido no escapado correctamente",
                FindingSeverity.MEDIUM
        );

        Finding foundFinding = findingService
                .findById(createdFinding.id())
                .orElseThrow();

        assertEquals(createdFinding, foundFinding);
    }

    @Test
    void deberiaDevolverResultadoVacioSiElHallazgoNoExiste() {
        assertTrue(
                findingService.findById(UUID.randomUUID()).isEmpty()
        );
    }

    @Test
    void deberiaLanzarExcepcionSiElHallazgoNoExiste() {
        assertThrows(
                FindingNotFoundException.class,
                () -> findingService.getById(UUID.randomUUID())
        );
    }
}