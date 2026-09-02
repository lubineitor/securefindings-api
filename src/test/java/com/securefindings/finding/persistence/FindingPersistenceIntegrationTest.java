package com.securefindings.finding.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.securefindings.finding.application.FindingService;
import com.securefindings.finding.domain.Finding;
import com.securefindings.finding.domain.FindingSeverity;
import com.securefindings.finding.domain.FindingStatus;

@Testcontainers
@SpringBootTest
class FindingPersistenceIntegrationTest {

    @SuppressWarnings("resource")
    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("securefindings_test")
            .withUsername("securefindings_test")
            .withPassword("securefindings_test");

    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private FindingService findingService;

    @Autowired
    private FindingRepository findingRepository;

    @BeforeEach
    void limpiarBaseDeDatos() {
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
}