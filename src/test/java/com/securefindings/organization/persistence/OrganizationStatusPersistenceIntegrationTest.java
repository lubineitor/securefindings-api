package com.securefindings.organization.persistence;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.securefindings.organization.domain.Organization;
import com.securefindings.organization.domain.OrganizationStatus;

@Testcontainers
@SpringBootTest
class OrganizationStatusPersistenceIntegrationTest {

    private static final UUID ORGANIZATION_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000201");

    @SuppressWarnings("resource")
    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer(
            "postgres:17-alpine")
            .withDatabaseName("securefindings_test")
            .withUsername("securefindings_test")
            .withPassword("securefindings_test");

    @Autowired
    private OrganizationRepository organizationRepository;

    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    void limpiarOrganizacion() {
        organizationRepository.deleteById(ORGANIZATION_ID);
        organizationRepository.flush();
    }

    @Test
    void persisteYFiltraElEstadoDeLaOrganizacion() {
        Instant createdAt = Instant.parse("2026-10-01T10:00:00Z");

        Organization active = new Organization(
                ORGANIZATION_ID,
                "Secure Findings",
                "secure-findings-status-test",
                createdAt);

        organizationRepository.saveAndFlush(new OrganizationEntity(active));

        assertTrue(organizationRepository.existsByIdAndStatus(
                ORGANIZATION_ID,
                OrganizationStatus.ACTIVE));
        assertFalse(organizationRepository.existsByIdAndStatus(
                ORGANIZATION_ID,
                OrganizationStatus.SUSPENDED));

        Organization suspended = new Organization(
                ORGANIZATION_ID,
                "Secure Findings",
                "secure-findings-status-test",
                createdAt,
                OrganizationStatus.SUSPENDED);

        organizationRepository.saveAndFlush(new OrganizationEntity(suspended));

        assertFalse(organizationRepository.existsByIdAndStatus(
                ORGANIZATION_ID,
                OrganizationStatus.ACTIVE));
        assertTrue(organizationRepository.existsByIdAndStatus(
                ORGANIZATION_ID,
                OrganizationStatus.SUSPENDED));
    }
}