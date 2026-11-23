package com.securefindings.organization.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
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

import com.securefindings.organization.domain.Organization;
import com.securefindings.organization.domain.OrganizationStatus;
import com.securefindings.organization.domain.OrganizationStatusAuditAction;
import com.securefindings.organization.domain.OrganizationStatusAuditEvent;

@Testcontainers
@SpringBootTest
class OrganizationStatusAuditPersistenceIntegrationTest {

    private static final UUID ORGANIZATION_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000301");

    @SuppressWarnings("resource")
    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer(
            "postgres:17-alpine")
            .withDatabaseName("securefindings_test")
            .withUsername("securefindings_test")
            .withPassword("securefindings_test");

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationStatusAuditRepository auditRepository;

    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    void limpiarDatosDePrueba() {
        auditRepository.deleteAll();
        organizationRepository.deleteById(ORGANIZATION_ID);
        organizationRepository.flush();
    }

    @Test
    void deberiaPersistirLaAuditoriaDeCambioDeEstadoEnPostgreSQL() {
        Instant createdAt = Instant.parse("2026-10-01T10:00:00Z");

        Organization organization = new Organization(
                ORGANIZATION_ID,
                "Secure Findings",
                "secure-findings-audit-test",
                createdAt,
                OrganizationStatus.SUSPENDED);

        organizationRepository.saveAndFlush(new OrganizationEntity(organization));

        OrganizationStatusAuditEvent event = new OrganizationStatusAuditEvent(
                UUID.randomUUID(),
                ORGANIZATION_ID,
                OrganizationStatusAuditAction.SUSPENDED,
                "admin",
                Instant.parse("2026-10-02T10:00:00Z"),
                "org-status-456");

        auditRepository.saveAndFlush(new OrganizationStatusAuditEntity(event));

        Page<OrganizationStatusAuditEntity> result = auditRepository.findByOrganizationId(
                ORGANIZATION_ID,
                PageRequest.of(
                        0,
                        10,
                        Sort.by(
                                Sort.Order.desc("occurredAt"),
                                Sort.Order.desc("id"))));

        assertEquals(1, result.getTotalElements());
        assertEquals(OrganizationStatusAuditAction.SUSPENDED,
                result.getContent().get(0).getAction());
        assertEquals("admin", result.getContent().get(0).getActor());
        assertEquals("org-status-456",
                result.getContent().get(0).getRequestId());

        assertEquals(ORGANIZATION_ID, result.getContent().get(0).getOrganizationId());
    }
}