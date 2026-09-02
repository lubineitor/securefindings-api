package com.securefindings.finding.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.securefindings.audit.application.AuditService;
import com.securefindings.audit.domain.AuditAction;
import com.securefindings.finding.domain.Finding;
import com.securefindings.finding.domain.FindingSeverity;
import com.securefindings.finding.domain.FindingStatus;
import com.securefindings.finding.persistence.FindingEntity;
import com.securefindings.finding.persistence.FindingRepository;

@ExtendWith(MockitoExtension.class)
class FindingServiceTest {

        @Mock
        private FindingRepository findingRepository;

        @Mock
        private AuditService auditService;

        @InjectMocks
        private FindingService findingService;

        @Test
        void deberiaCrearYGuardarUnHallazgo() {
                when(findingRepository.save(any(FindingEntity.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                Finding finding = findingService.create(
                                "SQL Injection",
                                "Entrada de usuario sin validar",
                                FindingSeverity.HIGH);

                assertEquals("SQL Injection", finding.title());
                assertEquals(FindingSeverity.HIGH, finding.severity());
                assertEquals(FindingStatus.OPEN, finding.status());

                verify(findingRepository).save(any(FindingEntity.class));

                verify(auditService).register(
                                eq(finding.id()),
                                eq(AuditAction.CREATED),
                                anyString());
        }

        @Test
        void deberiaListarLosHallazgosGuardados() {
                Finding firstFinding = Finding.create(
                                "SQL Injection",
                                "Entrada sin validar",
                                FindingSeverity.HIGH);

                Finding secondFinding = Finding.create(
                                "XSS",
                                "Contenido sin escapar",
                                FindingSeverity.MEDIUM);

                when(findingRepository.findAll())
                                .thenReturn(List.of(
                                                new FindingEntity(firstFinding),
                                                new FindingEntity(secondFinding)));

                List<Finding> findings = findingService.findAll();

                assertEquals(
                                List.of(firstFinding, secondFinding),
                                findings);

                verify(findingRepository).findAll();
        }

        @Test
        void deberiaEncontrarUnHallazgoPorSuIdentificador() {
                Finding finding = Finding.create(
                                "Cross-Site Scripting",
                                "Contenido no escapado correctamente",
                                FindingSeverity.MEDIUM);

                when(findingRepository.findById(finding.id()))
                                .thenReturn(Optional.of(new FindingEntity(finding)));

                Finding foundFinding = findingService
                                .findById(finding.id())
                                .orElseThrow();

                assertEquals(finding, foundFinding);
        }

        @Test
        void deberiaDevolverResultadoVacioSiElHallazgoNoExiste() {
                UUID id = UUID.randomUUID();

                when(findingRepository.findById(id))
                                .thenReturn(Optional.empty());

                assertTrue(findingService.findById(id).isEmpty());
        }

        @Test
        void deberiaLanzarExcepcionSiElHallazgoNoExiste() {
                UUID id = UUID.randomUUID();

                when(findingRepository.findById(id))
                                .thenReturn(Optional.empty());

                assertThrows(
                                FindingNotFoundException.class,
                                () -> findingService.getById(id));
        }

        @Test
        void deberiaActualizarElEstadoDeUnHallazgo() {
                Finding finding = Finding.create(
                                "SQL Injection",
                                "Entrada sin validar",
                                FindingSeverity.HIGH);

                when(findingRepository.findById(finding.id()))
                                .thenReturn(Optional.of(new FindingEntity(finding)));

                when(findingRepository.save(any(FindingEntity.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                Finding updatedFinding = findingService.updateStatus(
                                finding.id(),
                                FindingStatus.RESOLVED);

                assertEquals(finding.id(), updatedFinding.id());
                assertEquals(FindingStatus.RESOLVED, updatedFinding.status());

                verify(findingRepository).save(any(FindingEntity.class));

                verify(auditService).register(
                                eq(finding.id()),
                                eq(AuditAction.UPDATED),
                                anyString());
        }

        @Test
        void deberiaActualizarLosDatosDeUnHallazgo() {
                Finding finding = Finding.create(
                                "SQL Injection",
                                "Descripción inicial",
                                FindingSeverity.MEDIUM);

                when(findingRepository.findById(finding.id()))
                                .thenReturn(Optional.of(new FindingEntity(finding)));

                when(findingRepository.save(any(FindingEntity.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                Finding updatedFinding = findingService.update(
                                finding.id(),
                                "SQL Injection corregido",
                                "Descripción actualizada",
                                FindingSeverity.HIGH);

                assertEquals("SQL Injection corregido", updatedFinding.title());
                assertEquals("Descripción actualizada", updatedFinding.description());
                assertEquals(FindingSeverity.HIGH, updatedFinding.severity());

                verify(findingRepository).save(any(FindingEntity.class));

                verify(auditService).register(
                                eq(finding.id()),
                                eq(AuditAction.UPDATED),
                                anyString());
        }

        @Test
        void deberiaEliminarUnHallazgoExistente() {
                UUID id = UUID.randomUUID();

                when(findingRepository.existsById(id))
                                .thenReturn(true);

                findingService.deleteById(id);

                verify(findingRepository).deleteById(id);

                verify(auditService).register(
                                eq(id),
                                eq(AuditAction.DELETED),
                                anyString());
        }

        @Test
        void deberiaLanzarExcepcionAlEliminarUnHallazgoInexistente() {
                UUID id = UUID.randomUUID();

                when(findingRepository.existsById(id))
                                .thenReturn(false);

                assertThrows(
                                FindingNotFoundException.class,
                                () -> findingService.deleteById(id));

                verify(findingRepository, never()).deleteById(id);

                verify(auditService, never()).register(
                                any(UUID.class),
                                any(AuditAction.class),
                                anyString());
        }
}