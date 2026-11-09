package com.securefindings.asset.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.securefindings.asset.domain.Asset;
import com.securefindings.asset.domain.AssetAuditAction;
import com.securefindings.asset.domain.AssetCriticality;
import com.securefindings.asset.domain.AssetEnvironment;
import com.securefindings.asset.domain.AssetStatus;
import com.securefindings.asset.domain.AssetSummary;
import com.securefindings.asset.domain.AssetType;
import com.securefindings.asset.persistence.AssetEntity;
import com.securefindings.asset.persistence.AssetRepository;
import com.securefindings.finding.persistence.FindingRepository;
import com.securefindings.security.OrganizationContext;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

        private static final UUID ORGANIZATION_ID = UUID.fromString(
                        "00000000-0000-0000-0000-000000000001");

        @Mock
        private AssetRepository assetRepository;

        @Mock
        private FindingRepository findingRepository;

        @Mock
        private OrganizationContext organizationContext;

        @InjectMocks
        private AssetService assetService;

        @Mock
        private AssetAuditService assetAuditService;

        @BeforeEach
        void configurarOrganizacion() {
                lenient()
                                .when(organizationContext.currentOrganizationId())
                                .thenReturn(ORGANIZATION_ID);
        }

        @Test
        void deberiaCrearUnActivoEnLaOrganizacionActual() {
                when(assetRepository.existsByOrganizationIdAndIdentifierIgnoreCase(
                                ORGANIZATION_ID,
                                "https://payments.example.test"))
                                .thenReturn(false);
                when(assetRepository.saveAndFlush(any(AssetEntity.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                Asset created = assetService.create(
                                "API pagos",
                                " https://payments.example.test ",
                                AssetType.API,
                                AssetEnvironment.PRODUCTION,
                                AssetCriticality.HIGH);

                assertEquals("https://payments.example.test", created.identifier());
                assertEquals(AssetStatus.ACTIVE, created.status());
                verify(assetAuditService).register(
                                created.id(),
                                AssetAuditAction.CREATED);

                ArgumentCaptor<AssetEntity> captor = ArgumentCaptor.forClass(AssetEntity.class);
                verify(assetRepository).saveAndFlush(captor.capture());
                assertEquals(ORGANIZATION_ID, captor.getValue().getOrganizationId());
                assertEquals("https://payments.example.test", captor.getValue().getIdentifier());
        }

        @Test
        void deberiaRechazarUnIdentificadorDuplicadoDentroDeLaOrganizacion() {
                when(assetRepository.existsByOrganizationIdAndIdentifierIgnoreCase(
                                ORGANIZATION_ID,
                                "payments-api"))
                                .thenReturn(true);

                assertThrows(
                                AssetIdentifierAlreadyExistsException.class,
                                () -> assetService.create(
                                                "API pagos",
                                                "payments-api",
                                                AssetType.API,
                                                AssetEnvironment.PRODUCTION,
                                                AssetCriticality.HIGH));

                verify(assetRepository, never()).saveAndFlush(any(AssetEntity.class));
        }

        @Test
        void deberiaListarActivosPaginadosYFiltradosPorOrganizacion() {
                Asset asset = sampleAsset(UUID.randomUUID(), "payments-api");
                when(assetRepository.findPageByOrganizationAndFilters(
                                eq(ORGANIZATION_ID),
                                eq("payments"),
                                eq(AssetType.API),
                                eq(AssetEnvironment.PRODUCTION),
                                eq(AssetCriticality.HIGH),
                                eq(AssetStatus.ACTIVE),
                                any(Pageable.class)))
                                .thenAnswer(invocation -> {
                                        Pageable pageable = invocation.getArgument(6);
                                        return new PageImpl<>(
                                                        List.of(new AssetEntity(asset, ORGANIZATION_ID)),
                                                        pageable,
                                                        1);
                                });

                var result = assetService.findPage(
                                0,
                                10,
                                " payments ",
                                AssetType.API,
                                AssetEnvironment.PRODUCTION,
                                AssetCriticality.HIGH,
                                AssetStatus.ACTIVE);

                assertEquals(List.of(asset), result.getContent());
                assertEquals(1, result.getTotalElements());
        }

        @Test
        void deberiaConstruirElResumenDeActivosConCerosYFiltrosDeFecha() {
                Instant createdFrom = Instant.parse("2026-10-01T00:00:00Z");
                Instant createdTo = Instant.parse("2026-10-31T23:59:59Z");

                AssetRepository.StatusCountProjection statusCount = org.mockito.Mockito.mock(
                                AssetRepository.StatusCountProjection.class);
                when(statusCount.getStatus()).thenReturn(AssetStatus.ACTIVE);
                when(statusCount.getTotal()).thenReturn(2L);

                AssetRepository.TypeCountProjection typeCount = org.mockito.Mockito.mock(
                                AssetRepository.TypeCountProjection.class);
                when(typeCount.getType()).thenReturn(AssetType.API);
                when(typeCount.getTotal()).thenReturn(2L);

                AssetRepository.EnvironmentCountProjection environmentCount = org.mockito.Mockito.mock(
                                AssetRepository.EnvironmentCountProjection.class);
                when(environmentCount.getEnvironment())
                                .thenReturn(AssetEnvironment.PRODUCTION);
                when(environmentCount.getTotal()).thenReturn(2L);

                AssetRepository.CriticalityCountProjection criticalityCount = org.mockito.Mockito.mock(
                                AssetRepository.CriticalityCountProjection.class);
                when(criticalityCount.getCriticality())
                                .thenReturn(AssetCriticality.HIGH);
                when(criticalityCount.getTotal()).thenReturn(2L);

                when(assetRepository.countStatusesByOrganization(
                                ORGANIZATION_ID,
                                createdFrom,
                                createdTo))
                                .thenReturn(List.of(statusCount));
                when(assetRepository.countTypesByOrganization(
                                ORGANIZATION_ID,
                                createdFrom,
                                createdTo))
                                .thenReturn(List.of(typeCount));
                when(assetRepository.countEnvironmentsByOrganization(
                                ORGANIZATION_ID,
                                createdFrom,
                                createdTo))
                                .thenReturn(List.of(environmentCount));
                when(assetRepository.countCriticalitiesByOrganization(
                                ORGANIZATION_ID,
                                createdFrom,
                                createdTo))
                                .thenReturn(List.of(criticalityCount));

                AssetSummary summary = assetService.getSummary(createdFrom, createdTo);

                assertEquals(2L, summary.total());
                assertEquals(2L, summary.byStatus().get(AssetStatus.ACTIVE));
                assertEquals(0L, summary.byStatus().get(AssetStatus.ARCHIVED));
                assertEquals(2L, summary.byType().get(AssetType.API));
                assertEquals(0L, summary.byType().get(AssetType.SERVER));
                assertEquals(2L, summary.byEnvironment().get(AssetEnvironment.PRODUCTION));
                assertEquals(2L, summary.byCriticality().get(AssetCriticality.HIGH));

                verify(assetRepository).countStatusesByOrganization(
                                ORGANIZATION_ID,
                                createdFrom,
                                createdTo);
                verify(assetRepository).countTypesByOrganization(
                                ORGANIZATION_ID,
                                createdFrom,
                                createdTo);
                verify(assetRepository).countEnvironmentsByOrganization(
                                ORGANIZATION_ID,
                                createdFrom,
                                createdTo);
                verify(assetRepository).countCriticalitiesByOrganization(
                                ORGANIZATION_ID,
                                createdFrom,
                                createdTo);
        }

        @Test
        void deberiaRechazarUnRangoDeFechasInvertidoEnElResumen() {
                Instant createdFrom = Instant.parse("2026-10-31T23:59:59Z");
                Instant createdTo = Instant.parse("2026-10-01T00:00:00Z");

                assertThrows(
                                IllegalArgumentException.class,
                                () -> assetService.getSummary(createdFrom, createdTo));
        }

        @Test
        void deberiaOcultarUnActivoDeOtraOrganizacion() {
                UUID assetId = UUID.randomUUID();
                when(assetRepository.findByIdAndOrganizationId(assetId, ORGANIZATION_ID))
                                .thenReturn(Optional.empty());

                assertThrows(AssetNotFoundException.class, () -> assetService.getById(assetId));
        }

        @Test
        void deberiaActualizarSoloLosCamposRecibidos() {
                Asset current = sampleAsset(UUID.randomUUID(), "payments-api");
                when(assetRepository.findByIdAndOrganizationId(current.id(), ORGANIZATION_ID))
                                .thenReturn(Optional.of(new AssetEntity(current, ORGANIZATION_ID)));
                when(assetRepository.existsByOrganizationIdAndIdentifierIgnoreCaseAndIdNot(
                                ORGANIZATION_ID,
                                "payments-api",
                                current.id()))
                                .thenReturn(false);
                when(assetRepository.saveAndFlush(any(AssetEntity.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                Asset updated = assetService.update(
                                current.id(),
                                "API de pagos",
                                null,
                                null,
                                null,
                                null,
                                AssetStatus.ARCHIVED);

                assertEquals("API de pagos", updated.name());
                assertEquals("payments-api", updated.identifier());
                assertEquals(AssetType.API, updated.type());
                assertEquals(AssetStatus.ARCHIVED, updated.status());
                verify(assetAuditService).register(
                                current.id(),
                                AssetAuditAction.ARCHIVED);
        }

        @Test
        void noDeberiaEliminarUnActivoConHallazgos() {
                Asset asset = sampleAsset(UUID.randomUUID(), "payments-api");
                when(assetRepository.findByIdAndOrganizationId(asset.id(), ORGANIZATION_ID))
                                .thenReturn(Optional.of(new AssetEntity(asset, ORGANIZATION_ID)));
                when(findingRepository.existsByOrganizationIdAndAssetId(ORGANIZATION_ID, asset.id()))
                                .thenReturn(true);

                assertThrows(AssetHasFindingsException.class, () -> assetService.delete(asset.id()));
                verify(assetAuditService, never()).register(
                                asset.id(),
                                AssetAuditAction.DELETED);
                verify(assetRepository, never()).delete(any(AssetEntity.class));
        }

        @Test
        void deberiaEliminarUnActivoSinHallazgos() {
                Asset asset = sampleAsset(UUID.randomUUID(), "payments-api");
                AssetEntity entity = new AssetEntity(asset, ORGANIZATION_ID);
                when(assetRepository.findByIdAndOrganizationId(asset.id(), ORGANIZATION_ID))
                                .thenReturn(Optional.of(entity));
                when(findingRepository.existsByOrganizationIdAndAssetId(ORGANIZATION_ID, asset.id()))
                                .thenReturn(false);

                assetService.delete(asset.id());

                verify(assetRepository).delete(entity);
                verify(assetRepository).flush();
                verify(assetAuditService).register(
                                asset.id(),
                                AssetAuditAction.DELETED);
        }

        @Test
        void deberiaExportarActivosFiltradosDeLaOrganizacionActual() {
                Asset asset = sampleAsset(UUID.randomUUID(), "payments-api");

                when(assetRepository.findPageByOrganizationAndFilters(
                                eq(ORGANIZATION_ID),
                                eq("payments"),
                                eq(AssetType.API),
                                eq(AssetEnvironment.PRODUCTION),
                                eq(AssetCriticality.HIGH),
                                eq(AssetStatus.ACTIVE),
                                any(Pageable.class)))
                                .thenAnswer(invocation -> {
                                        Pageable pageable = invocation.getArgument(6);
                                        return new PageImpl<>(
                                                        List.of(new AssetEntity(asset, ORGANIZATION_ID)),
                                                        pageable,
                                                        1);
                                });

                List<Asset> exported = assetService.findForExport(
                                " payments ",
                                AssetType.API,
                                AssetEnvironment.PRODUCTION,
                                AssetCriticality.HIGH,
                                AssetStatus.ACTIVE);

                assertEquals(List.of(asset), exported);

                ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

                verify(assetRepository).findPageByOrganizationAndFilters(
                                eq(ORGANIZATION_ID),
                                eq("payments"),
                                eq(AssetType.API),
                                eq(AssetEnvironment.PRODUCTION),
                                eq(AssetCriticality.HIGH),
                                eq(AssetStatus.ACTIVE),
                                pageableCaptor.capture());

                assertEquals(
                                AssetService.MAX_EXPORT_ROWS + 1,
                                pageableCaptor.getValue().getPageSize());
        }

        @Test
        void deberiaRechazarLaExportacionQueSuperaElLimite() {
                Asset asset = sampleAsset(UUID.randomUUID(), "payments-api");

                when(assetRepository.findPageByOrganizationAndFilters(
                                eq(ORGANIZATION_ID),
                                isNull(),
                                isNull(),
                                isNull(),
                                isNull(),
                                isNull(),
                                any(Pageable.class)))
                                .thenAnswer(invocation -> new PageImpl<>(
                                                List.of(new AssetEntity(asset, ORGANIZATION_ID)),
                                                invocation.getArgument(6),
                                                AssetService.MAX_EXPORT_ROWS + 1L));

                AssetExportLimitExceededException exception = assertThrows(
                                AssetExportLimitExceededException.class,
                                () -> assetService.findForExport(
                                                null,
                                                null,
                                                null,
                                                null,
                                                null));

                assertEquals(AssetService.MAX_EXPORT_ROWS, exception.maximumRows());
        }

        @Test
        void deberiaRegistrarLaReactivacionDeUnActivo() {
                Asset activeAsset = sampleAsset(UUID.randomUUID(), "payments-api");

                Asset archivedAsset = new Asset(
                                activeAsset.id(),
                                activeAsset.name(),
                                activeAsset.identifier(),
                                activeAsset.type(),
                                activeAsset.environment(),
                                activeAsset.criticality(),
                                AssetStatus.ARCHIVED,
                                activeAsset.createdAt(),
                                activeAsset.updatedAt());

                when(assetRepository.findByIdAndOrganizationId(
                                archivedAsset.id(),
                                ORGANIZATION_ID))
                                .thenReturn(Optional.of(new AssetEntity(
                                                archivedAsset,
                                                ORGANIZATION_ID)));

                when(assetRepository.existsByOrganizationIdAndIdentifierIgnoreCaseAndIdNot(
                                ORGANIZATION_ID,
                                archivedAsset.identifier(),
                                archivedAsset.id()))
                                .thenReturn(false);

                when(assetRepository.saveAndFlush(any(AssetEntity.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                Asset restored = assetService.update(
                                archivedAsset.id(),
                                null,
                                null,
                                null,
                                null,
                                null,
                                AssetStatus.ACTIVE);

                assertEquals(AssetStatus.ACTIVE, restored.status());
                verify(assetAuditService).register(
                                archivedAsset.id(),
                                AssetAuditAction.RESTORED);
        }

        private Asset sampleAsset(UUID id, String identifier) {
                Instant now = Instant.parse("2026-10-01T10:00:00Z");
                return new Asset(
                                id,
                                "API pagos",
                                identifier,
                                AssetType.API,
                                AssetEnvironment.PRODUCTION,
                                AssetCriticality.HIGH,
                                AssetStatus.ACTIVE,
                                now,
                                now);
        }
}