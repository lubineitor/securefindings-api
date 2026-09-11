package com.securefindings.finding.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.securefindings.audit.application.AuditService;
import com.securefindings.audit.domain.AuditAction;
import com.securefindings.audit.persistence.FindingAuditEntity;
import com.securefindings.audit.persistence.FindingAuditRepository;
import com.securefindings.finding.application.FindingService;
import com.securefindings.finding.domain.Finding;
import com.securefindings.finding.domain.FindingSeverity;
import com.securefindings.finding.domain.FindingStatus;

@Testcontainers
@SpringBootTest
class FindingPersistenceIntegrationTest {

        private static final UUID ORGANIZATION_ID = UUID.fromString(
                        "00000000-0000-0000-0000-000000000001");

        @Autowired
        private AuditService auditService;

        @SuppressWarnings("resource")
        @Container
        static final PostgreSQLContainer postgres = new PostgreSQLContainer(
                        "postgres:17-alpine")
                        .withDatabaseName("securefindings_test")
                        .withUsername("securefindings_test")
                        .withPassword("securefindings_test");

        @DynamicPropertySource
        static void registerPostgresProperties(
                        DynamicPropertyRegistry registry) {

                registry.add("spring.datasource.url", postgres::getJdbcUrl);
                registry.add("spring.datasource.username", postgres::getUsername);
                registry.add("spring.datasource.password", postgres::getPassword);
        }

        @Autowired
        private FindingService findingService;

        @Autowired
        private FindingRepository findingRepository;

        @Autowired
        private FindingAuditRepository findingAuditRepository;

        @BeforeEach
        void limpiarBaseDeDatos() {
                findingAuditRepository.deleteAll();
                findingRepository.deleteAll();
        }

        @Test
        void deberiaPersistirYRecuperarUnHallazgoEnPostgreSQL() {
                Finding createdFinding = findingService.create(
                                "SQL Injection de integración",
                                "Hallazgo persistido en PostgreSQL",
                                FindingSeverity.HIGH);

                Finding recoveredFinding = findingService
                                .findById(createdFinding.id())
                                .orElseThrow();

                assertEquals(createdFinding.id(), recoveredFinding.id());
                assertEquals(createdFinding.title(), recoveredFinding.title());
                assertEquals(createdFinding.description(), recoveredFinding.description());
                assertEquals(FindingSeverity.HIGH, recoveredFinding.severity());
                assertEquals(FindingStatus.OPEN, recoveredFinding.status());
        }

        @Test
        void deberiaActualizarElEstadoDeUnHallazgoPersistido() {
                Finding createdFinding = findingService.create(
                                "Cross-Site Scripting",
                                "Contenido sin escapar",
                                FindingSeverity.MEDIUM);

                Finding updatedFinding = findingService.updateStatus(
                                createdFinding.id(),
                                FindingStatus.IN_PROGRESS);

                assertEquals(FindingStatus.IN_PROGRESS, updatedFinding.status());

                Finding recoveredFinding = findingService
                                .findById(createdFinding.id())
                                .orElseThrow();

                assertEquals(FindingStatus.IN_PROGRESS, recoveredFinding.status());
        }

        @Test
        void deberiaListarLosHallazgosPersistidos() {
                Finding firstFinding = findingService.create(
                                "SQL Injection",
                                "Consulta sin parametrizar",
                                FindingSeverity.CRITICAL);

                Finding secondFinding = findingService.create(
                                "Cross-Site Scripting",
                                "Salida sin escapar",
                                FindingSeverity.HIGH);

                List<Finding> findings = findingService.findAll();

                assertEquals(2, findings.size());
                assertTrue(findings.stream()
                                .anyMatch(finding -> finding.id().equals(firstFinding.id())));
                assertTrue(findings.stream()
                                .anyMatch(finding -> finding.id().equals(secondFinding.id())));
        }

        @Test
        void deberiaEliminarUnHallazgoPersistido() {
                Finding createdFinding = findingService.create(
                                "Configuración insegura",
                                "Credenciales expuestas",
                                FindingSeverity.HIGH);

                findingService.deleteById(createdFinding.id());

                assertTrue(findingService
                                .findById(createdFinding.id())
                                .isEmpty());
        }

        @Test
        void deberiaPersistirLaAuditoriaDeLasOperaciones() {
                Finding createdFinding = findingService.create(
                                "SQL Injection auditado",
                                "Hallazgo para comprobar auditoría",
                                FindingSeverity.HIGH);

                findingService.updateStatus(
                                createdFinding.id(),
                                FindingStatus.IN_PROGRESS);

                findingService.deleteById(createdFinding.id());

                List<FindingAuditEntity> auditEvents = findingAuditRepository
                                .findByFindingIdAndOrganizationIdOrderByOccurredAtAsc(
                                                createdFinding.id(),
                                                ORGANIZATION_ID);

                assertEquals(3, auditEvents.size());

                assertEquals(
                                List.of(
                                                AuditAction.CREATED,
                                                AuditAction.UPDATED,
                                                AuditAction.DELETED),
                                auditEvents.stream()
                                                .map(event -> Objects.requireNonNull(
                                                                event,
                                                                "El repositorio devolvió un evento de auditoría nulo")
                                                                .getAction())
                                                .toList());

                assertTrue(auditEvents.stream()
                                .allMatch(event -> "system".equals(event.getActor())));

                assertTrue(auditEvents.stream()
                                .allMatch(event -> event.getOccurredAt() != null));

                assertTrue(findingService
                                .findById(createdFinding.id())
                                .isEmpty());
        }

        @Test
        void deberiaBuscarHallazgosPersistidosPorTituloYDescripcion() {
                Finding titleMatch = findingService.create(
                                "SQL Injection",
                                "Consulta sin parametrizar",
                                FindingSeverity.HIGH);

                Finding descriptionMatch = findingService.create(
                                "Configuración insegura",
                                "El hallazgo contiene referencias a SQL",
                                FindingSeverity.MEDIUM);

                var results = findingService.findPage(
                                0,
                                20,
                                " SQL ",
                                null,
                                null);

                assertEquals(2, results.getTotalElements());

                assertTrue(results.getContent()
                                .stream()
                                .anyMatch(finding -> finding.id()
                                                .equals(titleMatch.id())));

                assertTrue(results.getContent()
                                .stream()
                                .anyMatch(finding -> finding.id()
                                                .equals(descriptionMatch.id())));
        }

        @Test
        void deberiaPaginarLaAuditoriaPersistidaEnPostgreSQL() {
                Finding createdFinding = findingService.create(
                                "SQL Injection",
                                "Consulta sin parametrizar",
                                FindingSeverity.HIGH);

                findingService.updateStatus(
                                createdFinding.id(),
                                FindingStatus.IN_PROGRESS);

                Page<FindingAuditEntity> firstPage = auditService
                                .findPageByFindingId(
                                                createdFinding.id(),
                                                0,
                                                1);

                Page<FindingAuditEntity> secondPage = auditService
                                .findPageByFindingId(
                                                createdFinding.id(),
                                                1,
                                                1);

                assertEquals(1, firstPage.getContent().size());
                assertEquals(2, firstPage.getTotalElements());
                assertEquals(2, firstPage.getTotalPages());
                assertEquals(0, firstPage.getNumber());
                assertEquals(1, firstPage.getSize());
                assertEquals(AuditAction.CREATED,
                                firstPage.getContent().get(0).getAction());
                assertEquals("system",
                                firstPage.getContent().get(0).getActor());
                assertEquals(true, firstPage.isFirst());
                assertEquals(false, firstPage.isLast());

                assertEquals(1, secondPage.getContent().size());
                assertEquals(2, secondPage.getTotalElements());
                assertEquals(2, secondPage.getTotalPages());
                assertEquals(1, secondPage.getNumber());
                assertEquals(AuditAction.UPDATED,
                                secondPage.getContent().get(0).getAction());
                assertEquals("system",
                                secondPage.getContent().get(0).getActor());
                assertEquals(false, secondPage.isFirst());
                assertEquals(true, secondPage.isLast());
        }

        @Test
        void deberiaOrdenarLosHallazgosPorTituloEnPostgreSQL() {

                findingRepository.save(
                                new FindingEntity(
                                                Finding.create(
                                                                "Zeta",
                                                                "Descripción Z",
                                                                FindingSeverity.MEDIUM),
                                                ORGANIZATION_ID));

                findingRepository.save(
                                new FindingEntity(
                                                Finding.create(
                                                                "Alpha",
                                                                "Descripción A",
                                                                FindingSeverity.MEDIUM),
                                                ORGANIZATION_ID));

                Page<FindingEntity> result = findingRepository.findPageByFilters(
                                ORGANIZATION_ID,
                                null,
                                null,
                                null,
                                PageRequest.of(
                                                0,
                                                20,
                                                Sort.by(
                                                                Sort.Order.asc("title"),
                                                                Sort.Order.asc("id"))));

                assertEquals(2, result.getTotalElements());
                assertEquals(
                                "Alpha",
                                result.getContent().get(0).getTitle());
                assertEquals(
                                "Zeta",
                                result.getContent().get(1).getTitle());
        }
}