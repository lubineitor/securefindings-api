package com.securefindings.finding.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

    @Test
    void deberiaPersistirYRecuperarUnHallazgoEnPostgreSQL() {
        Finding createdFinding = findingService.create(
                "SQL Injection de integración",
                "Hallazgo persistido en un PostgreSQL temporal",
                FindingSeverity.HIGH);

        Finding recoveredFinding = findingService
                .findById(createdFinding.id())
                .orElseThrow();

        assertEquals(createdFinding.id(), recoveredFinding.id());
        assertEquals(createdFinding.title(), recoveredFinding.title());
        assertEquals(createdFinding.description(), recoveredFinding.description());
        assertEquals(FindingSeverity.HIGH, recoveredFinding.severity());
        assertEquals(FindingStatus.OPEN, recoveredFinding.status());
        assertNotNull(recoveredFinding.createdAt());
        assertNotNull(recoveredFinding.updatedAt());
    }
}