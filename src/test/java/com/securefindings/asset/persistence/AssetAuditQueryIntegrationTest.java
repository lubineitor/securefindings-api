package com.securefindings.asset.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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

import com.securefindings.asset.application.AssetAuditQueryService;
import com.securefindings.asset.application.AssetService;
import com.securefindings.asset.domain.Asset;
import com.securefindings.asset.domain.AssetAuditAction;
import com.securefindings.asset.domain.AssetAuditEvent;
import com.securefindings.asset.domain.AssetCriticality;
import com.securefindings.asset.domain.AssetEnvironment;
import com.securefindings.asset.domain.AssetType;
import com.securefindings.asset.domain.AssetStatus;
import com.securefindings.organization.domain.Organization;
import com.securefindings.organization.persistence.OrganizationEntity;
import com.securefindings.organization.persistence.OrganizationRepository;

@Testcontainers
@SpringBootTest
class AssetAuditQueryIntegrationTest {

        private static final UUID OTHER_ORGANIZATION_ID = UUID.fromString(
                        "00000000-0000-0000-0000-000000000901");

        @SuppressWarnings("resource")
        @Container
        static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine")
                        .withDatabaseName("securefindings_asset_audit_test")
                        .withUsername("securefindings_test")
                        .withPassword("securefindings_test");

        @DynamicPropertySource
        static void registerPostgresProperties(DynamicPropertyRegistry registry) {
                registry.add("spring.datasource.url", postgres::getJdbcUrl);
                registry.add("spring.datasource.username", postgres::getUsername);
                registry.add("spring.datasource.password", postgres::getPassword);
        }

        @Autowired
        private AssetService assetService;

        @Autowired
        private AssetRepository assetRepository;

        @Autowired
        private AssetAuditRepository assetAuditRepository;

        @Autowired
        private AssetAuditQueryService assetAuditQueryService;

        @Autowired
        private OrganizationRepository organizationRepository;

        @BeforeEach
        void limpiarDatos() {
                assetAuditRepository.deleteAll();
                assetRepository.deleteAll();
                organizationRepository.deleteById(OTHER_ORGANIZATION_ID);
        }

        @Test
        void deberiaConsultarElHistorialAunqueElActivoEsteEliminado() {
                Asset asset = assetService.create(
                                "API de pagos",
                                "payments-audit-test",
                                AssetType.API,
                                AssetEnvironment.PRODUCTION,
                                AssetCriticality.HIGH);

                assetService.update(
                                asset.id(),
                                null,
                                null,
                                null,
                                null,
                                null,
                                AssetStatus.ARCHIVED);

                assetService.delete(asset.id());

                var page = assetAuditQueryService.findPageByAssetId(
                                asset.id(),
                                0,
                                20,
                                null,
                                null,
                                null,
                                null,
                                null);

                Set<AssetAuditAction> actions = Set.copyOf(
                                page.getContent()
                                                .stream()
                                                .map(event -> Objects.requireNonNull(
                                                                event,
                                                                "El repositorio devolvió un evento nulo")
                                                                .getAction())
                                                .toList());

                assertEquals(3L, page.getTotalElements());
                assertEquals(
                                Set.of(
                                                AssetAuditAction.CREATED,
                                                AssetAuditAction.ARCHIVED,
                                                AssetAuditAction.DELETED),
                                actions);
        }

        @Test
        void noDeberiaDevolverEventosDeOtraOrganizacion() {
                Organization organization = new Organization(
                                OTHER_ORGANIZATION_ID,
                                "Organización de prueba de auditoría",
                                "auditoria-prueba",
                                Instant.parse("2026-10-01T10:00:00Z"));

                organizationRepository.saveAndFlush(
                                new OrganizationEntity(organization));

                UUID assetId = UUID.randomUUID();

                AssetAuditEvent otherOrganizationEvent = new AssetAuditEvent(
                                UUID.randomUUID(),
                                assetId,
                                OTHER_ORGANIZATION_ID,
                                AssetAuditAction.CREATED,
                                "otro-usuario",
                                Instant.parse("2026-10-10T10:00:00Z"),
                                "other-organization-request");

                assetAuditRepository.saveAndFlush(
                                new AssetAuditEntity(otherOrganizationEvent));

                var page = assetAuditQueryService.findPageByAssetId(
                                assetId,
                                0,
                                20,
                                null,
                                null,
                                null,
                                null,
                                null);

                assertEquals(0L, page.getTotalElements());
                assertEquals(List.of(), page.getContent());
        }

        @Test
        void deberiaListarEventosDeActivosSoloDeLaOrganizacionActual() {
                Asset currentAsset = assetService.create(
                                "API de pagos de la organización actual",
                                "payments-current-organization",
                                AssetType.API,
                                AssetEnvironment.PRODUCTION,
                                AssetCriticality.HIGH);

                Organization otherOrganization = new Organization(
                                OTHER_ORGANIZATION_ID,
                                "Organización externa",
                                "auditoria-externa",
                                Instant.parse("2026-10-01T10:00:00Z"));

                organizationRepository.saveAndFlush(
                                new OrganizationEntity(otherOrganization));

                UUID otherAssetId = UUID.randomUUID();

                AssetAuditEvent otherOrganizationEvent = new AssetAuditEvent(
                                UUID.randomUUID(),
                                otherAssetId,
                                OTHER_ORGANIZATION_ID,
                                AssetAuditAction.CREATED,
                                "otro-usuario",
                                Instant.parse("2026-10-10T10:00:00Z"),
                                "other-organization-request");

                assetAuditRepository.saveAndFlush(
                                new AssetAuditEntity(otherOrganizationEvent));

                var page = assetAuditQueryService.findPageByOrganization(
                                0,
                                20,
                                null,
                                null,
                                null,
                                null,
                                null);

                assertEquals(1L, page.getTotalElements());
                assertEquals(
                                currentAsset.id(),
                                page.getContent().getFirst().getAssetId());
        }
}