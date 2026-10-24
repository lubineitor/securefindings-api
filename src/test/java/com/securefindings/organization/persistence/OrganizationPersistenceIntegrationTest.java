package com.securefindings.organization.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import com.securefindings.organization.application.OrganizationHasAssociatedDataException;
import com.securefindings.organization.domain.Organization;
import com.securefindings.finding.domain.Finding;
import com.securefindings.finding.domain.FindingSeverity;
import com.securefindings.finding.domain.FindingStatus;
import com.securefindings.finding.persistence.FindingEntity;
import com.securefindings.finding.persistence.FindingRepository;

@Testcontainers
@SpringBootTest
class OrganizationPersistenceIntegrationTest {

        private static final UUID NAME_MATCH_ID = UUID.fromString(
                        "00000000-0000-0000-0000-000000000101");

        private static final UUID SLUG_MATCH_ID = UUID.fromString(
                        "00000000-0000-0000-0000-000000000102");

        private static final UUID EMPTY_ORGANIZATION_ID = UUID.fromString(
                        "00000000-0000-0000-0000-000000000103");

        private static final UUID ORGANIZATION_WITH_FINDING_ID = UUID.fromString(
                        "00000000-0000-0000-0000-000000000104");

        private static final UUID FINDING_ID = UUID.fromString(
                        "00000000-0000-0000-0000-000000000105");

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
        private FindingRepository findingRepository;

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
                findingRepository.deleteById(FINDING_ID);
                findingRepository.flush();
                organizationRepository.deleteAllById(List.of(
                                NAME_MATCH_ID,
                                SLUG_MATCH_ID,
                                EMPTY_ORGANIZATION_ID,
                                ORGANIZATION_WITH_FINDING_ID));
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
                                nameResults.getContent().stream().map(organization -> organization.id()).toList());
                assertEquals(List.of(SLUG_MATCH_ID),
                                slugResults.getContent().stream().map(organization -> organization.id()).toList());
        }

        @Test
        void deberiaEliminarUnaOrganizacionVaciaEnPostgres() {
                Organization organization = new Organization(
                                EMPTY_ORGANIZATION_ID,
                                "Empty Organization",
                                "empty-organization",
                                Instant.parse("2026-10-01T10:00:00Z"));
                organizationRepository.saveAndFlush(new OrganizationEntity(organization));

                organizationService.deleteOrganization(EMPTY_ORGANIZATION_ID);

                assertFalse(organizationRepository.existsById(EMPTY_ORGANIZATION_ID));
        }

        @Test
        void deberiaConservarLaOrganizacionSiTieneHallazgosEnPostgres() {
                Instant now = Instant.parse("2026-10-01T10:00:00Z");
                Organization organization = new Organization(
                                ORGANIZATION_WITH_FINDING_ID,
                                "Organization with finding",
                                "organization-with-finding",
                                now);
                organizationRepository.saveAndFlush(new OrganizationEntity(organization));

                Finding finding = new Finding(
                                FINDING_ID,
                                "SQL injection",
                                "Unsafe query construction",
                                FindingSeverity.HIGH,
                                FindingStatus.OPEN,
                                now,
                                now);
                findingRepository.saveAndFlush(
                                new FindingEntity(finding, ORGANIZATION_WITH_FINDING_ID));

                assertThrows(
                                OrganizationHasAssociatedDataException.class,
                                () -> organizationService.deleteOrganization(
                                                ORGANIZATION_WITH_FINDING_ID));

                assertTrue(organizationRepository.existsById(ORGANIZATION_WITH_FINDING_ID));
                assertTrue(findingRepository.existsById(FINDING_ID));
        }
}
