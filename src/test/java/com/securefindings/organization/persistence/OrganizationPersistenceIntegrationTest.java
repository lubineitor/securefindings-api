package com.securefindings.organization.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.securefindings.organization.application.OrganizationService;
import com.securefindings.organization.domain.Organization;

@Testcontainers
@SpringBootTest
class OrganizationPersistenceIntegrationTest {

    private static final UUID NAME_MATCH_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000101");

    private static final UUID SLUG_MATCH_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000102");

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
    private OrganizationService organizationService;

    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    void limpiarOrganizacionesDePrueba() {
        organizationRepository.deleteAllById(List.of(NAME_MATCH_ID, SLUG_MATCH_ID));
        organizationRepository.flush();
    }

    @Test
    void deberiaBuscarPorNombreOSlugIgnorandoMayusculas() {
        Organization nameMatch = new Organization(
                NAME_MATCH_ID,
                "NeedleVisibleName Organization",
                "organization-name-target",
                Instant.parse("2026-10-01T10:00:00Z"));
        Organization slugMatch = new Organization(
                SLUG_MATCH_ID,
                "Different Organization Name",
                "specific-slug-needle",
                Instant.parse("2026-10-01T10:00:00Z"));

        organizationRepository.saveAllAndFlush(List.of(
                new OrganizationEntity(nameMatch),
                new OrganizationEntity(slugMatch)));

        Page<Organization> nameResults = organizationService.findOrganizations(
                0,
                20,
                "NEEDLEVISIBLENAME");
        Page<Organization> slugResults = organizationService.findOrganizations(
                0,
                20,
                "SLUG-NEEDLE");

        assertEquals(List.of(NAME_MATCH_ID),
                nameResults.getContent().stream()
                        .map(organization -> organization.id())
                        .toList());
        assertEquals(List.of(SLUG_MATCH_ID),
                slugResults.getContent().stream()
                        .map(organization -> organization.id())
                        .toList());
    }
}
