package com.securefindings.finding.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
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
import com.securefindings.finding.api.FindingSortDirection;
import com.securefindings.finding.api.FindingSortField;
import com.securefindings.finding.application.FindingService;
import com.securefindings.finding.domain.Finding;
import com.securefindings.finding.domain.FindingSeverity;
import com.securefindings.finding.domain.FindingStatus;
import com.securefindings.finding.domain.FindingSummary;
import com.securefindings.security.RequestCorrelationFilter;

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

                Page<Finding> findingPage = findingService.findPage(0, 20, null, null);
                List<Finding> findings = findingPage.getContent();

                assertEquals(2, findingPage.getTotalElements());

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
                                                ORGANIZATION_ID,
                                                PageRequest.of(0, 20))
                                .getContent();

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

        @Test
        void deberiaPersistirElRequestIdEnLaAuditoria() {
                Finding createdFinding;

                MDC.put(
                                RequestCorrelationFilter.MDC_KEY,
                                "integration-request-123");

                try {
                        createdFinding = findingService.create(
                                        "Hallazgo trazable",
                                        "Auditoría con identificador de petición",
                                        FindingSeverity.HIGH);
                } finally {
                        MDC.remove(RequestCorrelationFilter.MDC_KEY);
                }

                List<FindingAuditEntity> auditEvents = findingAuditRepository
                                .findByFindingIdAndOrganizationIdOrderByOccurredAtAsc(
                                                createdFinding.id(),
                                                ORGANIZATION_ID,
                                                PageRequest.of(0, 20))
                                .getContent();

                assertEquals(1, auditEvents.size());
                assertEquals(
                                "integration-request-123",
                                auditEvents.get(0).getRequestId());
        }

        @Test
        void deberiaFiltrarLaAuditoriaPersistidaPorAccionYRequestId() {
                Finding createdFinding;

                MDC.put(
                                RequestCorrelationFilter.MDC_KEY,
                                "audit-created-123");

                try {
                        createdFinding = findingService.create(
                                        "Hallazgo filtrable",
                                        "Auditoría con filtros",
                                        FindingSeverity.HIGH);

                        MDC.put(
                                        RequestCorrelationFilter.MDC_KEY,
                                        "audit-updated-456");

                        findingService.updateStatus(
                                        createdFinding.id(),
                                        FindingStatus.IN_PROGRESS);
                } finally {
                        MDC.remove(RequestCorrelationFilter.MDC_KEY);
                }

                Page<FindingAuditEntity> createdEvents = auditService
                                .findPageByFindingId(
                                                createdFinding.id(),
                                                0,
                                                20,
                                                AuditAction.CREATED,
                                                null);

                Page<FindingAuditEntity> requestEvents = auditService
                                .findPageByFindingId(
                                                createdFinding.id(),
                                                0,
                                                20,
                                                null,
                                                "audit-updated-456");

                Page<FindingAuditEntity> combinedEvents = auditService
                                .findPageByFindingId(
                                                createdFinding.id(),
                                                0,
                                                20,
                                                AuditAction.UPDATED,
                                                "audit-updated-456");

                assertEquals(1, createdEvents.getTotalElements());
                assertEquals(
                                AuditAction.CREATED,
                                createdEvents.getContent().get(0).getAction());

                assertEquals(1, requestEvents.getTotalElements());
                assertEquals(
                                "audit-updated-456",
                                requestEvents.getContent().get(0).getRequestId());

                assertEquals(1, combinedEvents.getTotalElements());
                assertEquals(
                                AuditAction.UPDATED,
                                combinedEvents.getContent().get(0).getAction());
        }

        @Test
        void deberiaFiltrarLaAuditoriaPersistidaPorActorEnPostgreSQL() {
                Finding finding = findingService.create(
                                "Hallazgo para filtrar auditoría",
                                "Prueba de búsqueda por actor",
                                FindingSeverity.MEDIUM);

                auditService.register(
                                finding.id(),
                                AuditAction.UPDATED,
                                "analista",
                                "actor-request-1");

                auditService.register(
                                finding.id(),
                                AuditAction.UPDATED,
                                "administrador",
                                "actor-request-2");

                Page<FindingAuditEntity> result = auditService.findPageByFindingId(
                                finding.id(),
                                0,
                                20,
                                AuditAction.UPDATED,
                                "actor-request-1",
                                "analista");

                assertEquals(1, result.getTotalElements());
                assertEquals("analista", result.getContent().get(0).getActor());
                assertEquals(
                                "actor-request-1",
                                result.getContent().get(0).getRequestId());
        }

        @Test
        void deberiaFiltrarLaAuditoriaPersistidaPorActor() {
                Finding createdFinding = findingService.create(
                                "Auditoría filtrada por actor",
                                "Hallazgo para probar el filtro de auditoría",
                                FindingSeverity.HIGH);

                auditService.register(
                                createdFinding.id(),
                                AuditAction.UPDATED,
                                "analista",
                                "actor-filter-request-1");

                auditService.register(
                                createdFinding.id(),
                                AuditAction.UPDATED,
                                "administrador",
                                "actor-filter-request-2");

                Page<FindingAuditEntity> analystEvents = auditService
                                .findPageByFindingId(
                                                createdFinding.id(),
                                                0,
                                                20,
                                                null,
                                                null,
                                                "analista");

                assertEquals(1, analystEvents.getTotalElements());
                assertEquals(
                                "analista",
                                analystEvents.getContent().get(0).getActor());
                assertEquals(
                                "actor-filter-request-1",
                                analystEvents.getContent().get(0).getRequestId());
        }

        @Test
        void deberiaFiltrarLosHallazgosPorUnRangoInclusivoDeFechaDeCreacion() {
                Instant createdFrom = Instant.parse("2026-10-01T10:00:00Z");
                Instant createdTo = Instant.parse("2026-10-03T10:00:00Z");

                Finding beforeRange = saveFindingAt(
                                "Antes del rango",
                                Instant.parse("2026-10-01T09:59:59Z"));
                Finding fromBoundary = saveFindingAt(
                                "Inicio del rango",
                                createdFrom);
                Finding withinRange = saveFindingAt(
                                "Dentro del rango",
                                Instant.parse("2026-10-02T10:00:00Z"));
                Finding toBoundary = saveFindingAt(
                                "Fin del rango",
                                createdTo);

                Page<Finding> results = findingService.findPage(
                                0,
                                20,
                                null,
                                null,
                                null,
                                FindingSortField.CREATED_AT,
                                FindingSortDirection.ASC,
                                createdFrom,
                                createdTo);

                assertEquals(3, results.getTotalElements());
                assertTrue(results.getContent().stream()
                                .anyMatch(finding -> finding.id().equals(fromBoundary.id())));
                assertTrue(results.getContent().stream()
                                .anyMatch(finding -> finding.id().equals(withinRange.id())));
                assertTrue(results.getContent().stream()
                                .anyMatch(finding -> finding.id().equals(toBoundary.id())));
                assertTrue(results.getContent().stream()
                                .noneMatch(finding -> finding.id().equals(beforeRange.id())));
        }

        private Finding saveFindingAt(
                        String title,
                        Instant createdAt) {

                Finding finding = new Finding(
                                UUID.randomUUID(),
                                title,
                                "Prueba del filtro por fecha de creación",
                                FindingSeverity.HIGH,
                                FindingStatus.OPEN,
                                createdAt,
                                createdAt);

                findingRepository.save(new FindingEntity(finding, ORGANIZATION_ID));
                return finding;
        }

        @Test
        void deberiaAgruparElResumenPersistidoPorEstadoYSeveridad() {
                Finding inProgress = findingService.create(
                                "Hallazgo en progreso",
                                "Prueba de resumen",
                                FindingSeverity.HIGH);
                findingService.updateStatus(
                                inProgress.id(),
                                FindingStatus.IN_PROGRESS);

                Finding resolved = findingService.create(
                                "Hallazgo resuelto",
                                "Prueba de resumen",
                                FindingSeverity.MEDIUM);
                findingService.updateStatus(
                                resolved.id(),
                                FindingStatus.RESOLVED);

                findingService.create(
                                "Hallazgo abierto",
                                "Prueba de resumen",
                                FindingSeverity.CRITICAL);

                Finding falsePositive = findingService.create(
                                "Falso positivo",
                                "Prueba de resumen",
                                FindingSeverity.LOW);
                findingService.updateStatus(
                                falsePositive.id(),
                                FindingStatus.FALSE_POSITIVE);

                FindingSummary summary = findingService.getSummary();

                assertEquals(4L, summary.total());
                assertEquals(1L, summary.byStatus().get(FindingStatus.OPEN));
                assertEquals(1L, summary.byStatus().get(FindingStatus.IN_PROGRESS));
                assertEquals(1L, summary.byStatus().get(FindingStatus.RESOLVED));
                assertEquals(1L, summary.byStatus().get(FindingStatus.FALSE_POSITIVE));

                assertEquals(1L, summary.bySeverity().get(FindingSeverity.LOW));
                assertEquals(1L, summary.bySeverity().get(FindingSeverity.MEDIUM));
                assertEquals(1L, summary.bySeverity().get(FindingSeverity.HIGH));
                assertEquals(1L, summary.bySeverity().get(FindingSeverity.CRITICAL));
        }

        @Test
        void deberiaFiltrarElResumenPorIntervaloInclusivoDeCreacion() {
                Instant createdFrom = Instant.parse("2000-01-02T00:00:00Z");
                Instant createdTo = Instant.parse("2000-01-04T00:00:00Z");

                saveFindingAt(
                                "Antes del intervalo",
                                Instant.parse("2000-01-01T23:59:59Z"));
                saveFindingAt("Inicio del intervalo", createdFrom);
                saveFindingAt(
                                "Dentro del intervalo",
                                Instant.parse("2000-01-03T00:00:00Z"));
                saveFindingAt("Fin del intervalo", createdTo);
                saveFindingAt(
                                "Después del intervalo",
                                Instant.parse("2000-01-04T00:00:01Z"));

                FindingSummary summary = findingService.getSummary(
                                createdFrom,
                                createdTo);

                assertEquals(3L, summary.total());
                assertEquals(3L, summary.byStatus().get(FindingStatus.OPEN));
                assertEquals(3L, summary.bySeverity().get(FindingSeverity.HIGH));
                assertEquals(0L, summary.bySeverity().get(FindingSeverity.LOW));
        }
}