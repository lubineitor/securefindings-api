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
                                FindingSeverity.HIGH);

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
                                FindingSeverity.MEDIUM);

                Finding foundFinding = findingService
                                .findById(createdFinding.id())
                                .orElseThrow();

                assertEquals(createdFinding, foundFinding);
        }

        @Test
        void deberiaDevolverResultadoVacioSiElHallazgoNoExiste() {
                assertTrue(
                                findingService.findById(UUID.randomUUID()).isEmpty());
        }

        @Test
        void deberiaLanzarExcepcionSiElHallazgoNoExiste() {
                assertThrows(
                                FindingNotFoundException.class,
                                () -> findingService.getById(UUID.randomUUID()));
        }

        @Test
        void deberiaActualizarElEstadoDeUnHallazgo() {
                Finding createdFinding = findingService.create(
                                "Cross-Site Scripting",
                                "Contenido no escapado correctamente",
                                FindingSeverity.MEDIUM);

                Finding updatedFinding = findingService.updateStatus(
                                createdFinding.id(),
                                FindingStatus.IN_PROGRESS);

                assertEquals(createdFinding.id(), updatedFinding.id());
                assertEquals(FindingStatus.IN_PROGRESS, updatedFinding.status());
                assertEquals(createdFinding.title(), updatedFinding.title());
                assertEquals(createdFinding.createdAt(), updatedFinding.createdAt());
                assertEquals(
                                updatedFinding,
                                findingService.getById(createdFinding.id()));
        }

        @Test
        void deberiaActualizarLosDatosDeUnHallazgo() {
                Finding createdFinding = findingService.create(
                                "SQL Injection",
                                "Entrada sin validar",
                                FindingSeverity.HIGH);

                Finding updatedFinding = findingService.update(
                                createdFinding.id(),
                                "SQL Injection corregida",
                                "Entrada validada correctamente",
                                FindingSeverity.CRITICAL);

                assertEquals(createdFinding.id(), updatedFinding.id());
                assertEquals(createdFinding.status(), updatedFinding.status());
                assertEquals(createdFinding.createdAt(), updatedFinding.createdAt());
                assertEquals("SQL Injection corregida", updatedFinding.title());
                assertEquals(
                                "Entrada validada correctamente",
                                updatedFinding.description());
                assertEquals(FindingSeverity.CRITICAL, updatedFinding.severity());
                assertEquals(
                                updatedFinding,
                                findingService.getById(createdFinding.id()));
        }

        @Test
        void deberiaEliminarUnHallazgo() {
                Finding finding = findingService.create(
                                "SQL Injection",
                                "Entrada sin validar",
                                FindingSeverity.HIGH);

                findingService.deleteById(finding.id());

                assertTrue(
                                findingService.findById(finding.id()).isEmpty());
        }

        @Test
        void deberiaLanzarExcepcionAlEliminarUnHallazgoInexistente() {
                assertThrows(
                                FindingNotFoundException.class,
                                () -> findingService.deleteById(UUID.randomUUID()));
        }
}