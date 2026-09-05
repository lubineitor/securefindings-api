package com.securefindings.finding.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.securefindings.audit.application.AuditService;
import com.securefindings.finding.domain.Finding;
import com.securefindings.finding.domain.FindingSeverity;
import com.securefindings.finding.domain.FindingStatus;
import com.securefindings.finding.persistence.FindingEntity;
import com.securefindings.finding.persistence.FindingRepository;
import com.securefindings.security.OrganizationContext;

@ExtendWith(MockitoExtension.class)
class FindingServiceTest {

        private static final UUID TEST_ORGANIZATION_ID = UUID.fromString(
                        "00000000-0000-0000-0000-000000000001");

        @Mock
        private FindingRepository findingRepository;

        @Mock
        private AuditService auditService;

        @Mock
        private OrganizationContext organizationContext;

        @InjectMocks
        private FindingService findingService;

        @BeforeEach
        void configurarOrganizacion() {
                when(organizationContext.currentOrganizationId())
                                .thenReturn(TEST_ORGANIZATION_ID);
        }

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

                when(findingRepository.findAllByOrganizationId(
                                TEST_ORGANIZATION_ID))
                                .thenReturn(List.of(
                                                new FindingEntity(firstFinding),
                                                new FindingEntity(secondFinding)));

                List<Finding> findings = findingService.findAll();

                assertEquals(
                                List.of(firstFinding, secondFinding),
                                findings);

                verify(findingRepository)
                                .findAllByOrganizationId(TEST_ORGANIZATION_ID);
        }

        @Test
        void deberiaEncontrarUnHallazgoPorSuIdentificador() {
                Finding finding = Finding.create(
                                "Cross-Site Scripting",
                                "Contenido no escapado correctamente",
                                FindingSeverity.MEDIUM);

                when(findingRepository.findByIdAndOrganizationId(
                                finding.id(),
                                TEST_ORGANIZATION_ID))
                                .thenReturn(Optional.of(
                                                new FindingEntity(finding)));

                Finding foundFinding = findingService
                                .findById(finding.id())
                                .orElseThrow();

                assertEquals(finding, foundFinding);
        }

        @Test
        void deberiaDevolverResultadoVacioSiElHallazgoNoExiste() {
                UUID id = UUID.randomUUID();

                when(findingRepository.findByIdAndOrganizationId(
                                id,
                                TEST_ORGANIZATION_ID))
                                .thenReturn(Optional.empty());

                assertTrue(findingService.findById(id).isEmpty());
        }

        @Test
        void deberiaLanzarExcepcionSiElHallazgoNoExiste() {
                UUID id = UUID.randomUUID();

                when(findingRepository.findByIdAndOrganizationId(
                                id,
                                TEST_ORGANIZATION_ID))
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

                when(findingRepository.findByIdAndOrganizationId(
                                finding.id(),
                                TEST_ORGANIZATION_ID))
                                .thenReturn(Optional.of(
                                                new FindingEntity(finding)));

                when(findingRepository.save(any(FindingEntity.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                Finding updatedFinding = findingService.updateStatus(
                                finding.id(),
                                FindingStatus.RESOLVED);

                assertEquals(finding.id(), updatedFinding.id());
                assertEquals(
                                FindingStatus.RESOLVED,
                                updatedFinding.status());

                verify(findingRepository).save(any(FindingEntity.class));
        }

        @Test
        void deberiaActualizarLosDatosDeUnHallazgo() {
                Finding finding = Finding.create(
                                "SQL Injection",
                                "Descripción inicial",
                                FindingSeverity.MEDIUM);

                when(findingRepository.findByIdAndOrganizationId(
                                finding.id(),
                                TEST_ORGANIZATION_ID))
                                .thenReturn(Optional.of(
                                                new FindingEntity(finding)));

                when(findingRepository.save(any(FindingEntity.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                Finding updatedFinding = findingService.update(
                                finding.id(),
                                "SQL Injection corregido",
                                "Descripción actualizada",
                                FindingSeverity.HIGH);

                assertEquals(
                                "SQL Injection corregido",
                                updatedFinding.title());

                assertEquals(
                                "Descripción actualizada",
                                updatedFinding.description());

                assertEquals(
                                FindingSeverity.HIGH,
                                updatedFinding.severity());

                verify(findingRepository).save(any(FindingEntity.class));
        }

        @Test
        void deberiaEliminarUnHallazgoExistente() {
                UUID id = UUID.randomUUID();

                when(findingRepository.existsByIdAndOrganizationId(
                                id,
                                TEST_ORGANIZATION_ID))
                                .thenReturn(true);

                findingService.deleteById(id);

                verify(findingRepository)
                                .deleteByIdAndOrganizationId(
                                                id,
                                                TEST_ORGANIZATION_ID);
        }

        @Test
        void deberiaLanzarExcepcionAlEliminarUnHallazgoInexistente() {
                UUID id = UUID.randomUUID();

                when(findingRepository.existsByIdAndOrganizationId(
                                id,
                                TEST_ORGANIZATION_ID))
                                .thenReturn(false);

                assertThrows(
                                FindingNotFoundException.class,
                                () -> findingService.deleteById(id));

                verify(
                                findingRepository,
                                never())
                                .deleteByIdAndOrganizationId(
                                                id,
                                                TEST_ORGANIZATION_ID);
        }

        @Test
        void noDebeEncontrarUnHallazgoDeOtraOrganizacion() {
                UUID organizationB = UUID.fromString(
                                "00000000-0000-0000-0000-000000000002");

                Finding finding = Finding.create(
                                "SQL Injection",
                                "Hallazgo perteneciente a otra organización",
                                FindingSeverity.HIGH);

                when(organizationContext.currentOrganizationId())
                                .thenReturn(organizationB);

                when(findingRepository.findByIdAndOrganizationId(
                                finding.id(),
                                organizationB))
                                .thenReturn(Optional.empty());

                assertTrue(
                                findingService.findById(finding.id()).isEmpty());

                verify(findingRepository)
                                .findByIdAndOrganizationId(
                                                finding.id(),
                                                organizationB);

                verify(findingRepository, never())
                                .findById(finding.id());
        }

        @Test
        void noDebeEliminarUnHallazgoDeOtraOrganizacion() {
                UUID organizationB = UUID.fromString(
                                "00000000-0000-0000-0000-000000000002");

                UUID findingId = UUID.randomUUID();

                when(organizationContext.currentOrganizationId())
                                .thenReturn(organizationB);

                when(findingRepository.existsByIdAndOrganizationId(
                                findingId,
                                organizationB))
                                .thenReturn(false);

                assertThrows(
                                FindingNotFoundException.class,
                                () -> findingService.deleteById(findingId));

                verify(findingRepository)
                                .existsByIdAndOrganizationId(
                                                findingId,
                                                organizationB);

                verify(findingRepository, never())
                                .deleteByIdAndOrganizationId(
                                                findingId,
                                                organizationB);
        }
}