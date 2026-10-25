package com.securefindings.asset.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.securefindings.asset.application.AssetNotFoundException;
import com.securefindings.asset.application.AssetService;
import com.securefindings.asset.domain.Asset;
import com.securefindings.asset.domain.AssetCriticality;
import com.securefindings.asset.domain.AssetEnvironment;
import com.securefindings.asset.domain.AssetType;
import com.securefindings.audit.persistence.FindingAuditRepository;
import com.securefindings.comment.persistence.FindingCommentRepository;
import com.securefindings.finding.api.FindingSortDirection;
import com.securefindings.finding.api.FindingSortField;
import com.securefindings.finding.application.FindingService;
import com.securefindings.finding.domain.Finding;
import com.securefindings.finding.domain.FindingSeverity;
import com.securefindings.finding.domain.FindingStatus;
import com.securefindings.finding.persistence.FindingEntity;
import com.securefindings.finding.persistence.FindingRepository;
import com.securefindings.organization.domain.Organization;
import com.securefindings.organization.persistence.OrganizationEntity;
import com.securefindings.organization.persistence.OrganizationRepository;

@Testcontainers
@SpringBootTest
class AssetPersistenceIntegrationTest {

    private static final UUID SYSTEM_ORGANIZATION_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000001");

    private static final UUID OTHER_ORGANIZATION_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000901");

    @SuppressWarnings("resource")
    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("securefindings_assets_test")
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
    private AssetService assetService;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private FindingService findingService;

    @Autowired
    private FindingRepository findingRepository;

    @Autowired
    private FindingAuditRepository findingAuditRepository;

    @Autowired
    private FindingCommentRepository findingCommentRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @BeforeEach
    void limpiarDatos() {
        findingAuditRepository.deleteAll();
        findingCommentRepository.deleteAll();
        findingRepository.deleteAll();
        assetRepository.deleteAll();
        organizationRepository.deleteById(OTHER_ORGANIZATION_ID);
    }

    @Test
    void deberiaPersistirActivoAsociarloYFiltrarHallazgosEnPostgres() {
        Asset asset = assetService.create(
                "API de pagos",
                "https://payments.example.test",
                AssetType.API,
                AssetEnvironment.PRODUCTION,
                AssetCriticality.HIGH);

        Finding finding = findingService.create(
                "SQL Injection",
                "Consulta sin parámetros",
                FindingSeverity.HIGH,
                asset.id());

        assertEquals(asset.id(), finding.assetId());
        assertEquals(
                asset.id(),
                findingRepository.findByIdAndOrganizationId(
                        finding.id(),
                        SYSTEM_ORGANIZATION_ID)
                        .orElseThrow()
                        .getAssetId());

        var filtered = findingService.findPage(
                0,
                20,
                null,
                null,
                null,
                FindingSortField.CREATED_AT,
                FindingSortDirection.DESC,
                null,
                null,
                asset.id());

        assertEquals(1, filtered.getTotalElements());
        assertEquals(finding.id(), filtered.getContent().getFirst().id());
    }

    @Test
    void deberiaListarActivosPorFiltrosSinMezclarOrganizaciones() {
        Asset currentAsset = assetService.create(
                "API pagos",
                "payments-api",
                AssetType.API,
                AssetEnvironment.PRODUCTION,
                AssetCriticality.HIGH);

        saveOtherOrganization();

        Asset otherAsset = Asset.create(
                "API pagos de otro tenant",
                "payments-api",
                AssetType.API,
                AssetEnvironment.PRODUCTION,
                AssetCriticality.HIGH);

        assetRepository.saveAndFlush(
                new AssetEntity(otherAsset, OTHER_ORGANIZATION_ID));

        var page = assetService.findPage(
                0,
                20,
                "PAYMENTS",
                AssetType.API,
                AssetEnvironment.PRODUCTION,
                AssetCriticality.HIGH,
                null);

        assertEquals(1, page.getTotalElements());
        assertEquals(currentAsset.id(), page.getContent().getFirst().id());
    }

    @Test
    void noDeberiaPermitirAsociarUnActivoDeOtraOrganizacion() {
        saveOtherOrganization();

        Asset otherAsset = Asset.create(
                "API de otra organización",
                "https://other.example.test",
                AssetType.API,
                AssetEnvironment.PRODUCTION,
                AssetCriticality.HIGH);

        assetRepository.saveAndFlush(
                new AssetEntity(otherAsset, OTHER_ORGANIZATION_ID));

        assertThrows(
                AssetNotFoundException.class,
                () -> findingService.create(
                        "Hallazgo aislado",
                        "No debe acceder al activo de otro tenant",
                        FindingSeverity.HIGH,
                        otherAsset.id()));
    }

    @Test
    void laBaseDeDatosDebeRechazarUnaRelacionEntreOrganizacionesDistintas() {
        saveOtherOrganization();

        Asset otherAsset = Asset.create(
                "API de otra organización",
                "https://other-db.example.test",
                AssetType.API,
                AssetEnvironment.PRODUCTION,
                AssetCriticality.HIGH);

        assetRepository.saveAndFlush(
                new AssetEntity(otherAsset, OTHER_ORGANIZATION_ID));

        Instant now = Instant.parse("2026-10-01T10:00:00Z");

        Finding invalidFinding = new Finding(
                UUID.randomUUID(),
                "Relación inválida",
                "El activo pertenece a otra organización",
                FindingSeverity.HIGH,
                FindingStatus.OPEN,
                now,
                now,
                otherAsset.id());

        assertThrows(
                DataIntegrityViolationException.class,
                () -> findingRepository.saveAndFlush(
                        new FindingEntity(
                                invalidFinding,
                                SYSTEM_ORGANIZATION_ID)));
    }

    @Test
    void deberiaAplicarLaUnicidadDelIdentificadorSinDistinguirMayusculas() {
        assetService.create(
                "API de pagos",
                "PAYMENTS-API",
                AssetType.API,
                AssetEnvironment.PRODUCTION,
                AssetCriticality.HIGH);

        assertTrue(
                assetRepository.existsByOrganizationIdAndIdentifierIgnoreCase(
                        SYSTEM_ORGANIZATION_ID,
                        "payments-api"));

        assertThrows(
                com.securefindings.asset.application.AssetIdentifierAlreadyExistsException.class,
                () -> assetService.create(
                        "Otra API de pagos",
                        "payments-api",
                        AssetType.API,
                        AssetEnvironment.STAGING,
                        AssetCriticality.MEDIUM));
    }

    private void saveOtherOrganization() {
        Organization organization = new Organization(
                OTHER_ORGANIZATION_ID,
                "Organización de prueba de activos",
                "activos-prueba",
                Instant.parse("2026-10-01T10:00:00Z"));

        organizationRepository.saveAndFlush(
                new OrganizationEntity(organization));
    }
}