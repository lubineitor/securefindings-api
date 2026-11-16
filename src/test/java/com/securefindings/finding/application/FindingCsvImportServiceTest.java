package com.securefindings.finding.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.securefindings.asset.domain.AssetStatus;
import com.securefindings.asset.persistence.AssetEntity;
import com.securefindings.asset.persistence.AssetRepository;
import com.securefindings.finding.api.FindingCsvImportResponse;
import com.securefindings.finding.api.FindingCsvParser;
import com.securefindings.finding.domain.Finding;
import com.securefindings.finding.domain.FindingSeverity;
import com.securefindings.security.OrganizationContext;

@ExtendWith(MockitoExtension.class)
class FindingCsvImportServiceTest {

        private static final UUID ORGANIZATION_ID = UUID.fromString(
                        "11111111-1111-1111-1111-111111111111");

        private static final String CSV_HEADER = "title,description,severity,assetId,dueAt\r\n";

        @Mock
        private AssetRepository assetRepository;

        @Mock
        private OrganizationContext organizationContext;

        @Mock
        private FindingService findingService;

        private FindingCsvImportService importService;

        @BeforeEach
        void setUp() {
                importService = new FindingCsvImportService(
                                new FindingCsvParser(),
                                assetRepository,
                                organizationContext,
                                findingService);
        }

        @Test
        void deberiaImportarUnHallazgoYConservarLaFechaLimite() {
                UUID assetId = UUID.fromString(
                                "22222222-2222-2222-2222-222222222222");
                Instant dueAt = Instant.parse("2026-12-01T09:00:00Z");

                AssetEntity activeAsset = mock(AssetEntity.class);
                when(activeAsset.getStatus()).thenReturn(AssetStatus.ACTIVE);

                when(organizationContext.currentOrganizationId())
                                .thenReturn(ORGANIZATION_ID);
                when(assetRepository.findByIdAndOrganizationId(
                                assetId,
                                ORGANIZATION_ID))
                                .thenReturn(Optional.of(activeAsset));

                when(findingService.create(
                                anyString(),
                                anyString(),
                                any(FindingSeverity.class),
                                nullable(UUID.class),
                                nullable(Instant.class)))
                                .thenAnswer(invocation -> Finding.create(
                                                invocation.getArgument(0, String.class),
                                                invocation.getArgument(1, String.class),
                                                invocation.getArgument(2, FindingSeverity.class),
                                                invocation.getArgument(3, UUID.class),
                                                invocation.getArgument(4, Instant.class)));

                FindingCsvImportResponse response = importService.importCsv(
                                csv("""
                                                "SQL Injection","Entrada sin validar",HIGH,%s,%s
                                                """.formatted(assetId, dueAt)));

                assertEquals(1, response.importedCount());
                assertEquals(1, response.findingIds().size());
                assertTrue(response.errors().isEmpty());

                verify(findingService).create(
                                "SQL Injection",
                                "Entrada sin validar",
                                FindingSeverity.HIGH,
                                assetId,
                                dueAt);
        }

        @Test
        void noDeberiaCrearHallazgosSiHayErroresDeValidacion() {
                FindingCsvImportResponse response = importService.importCsv(
                                csv(",Descripción,UNKNOWN,no-es-uuid,no-es-fecha"));

                assertEquals(0, response.importedCount());
                assertTrue(response.findingIds().isEmpty());
                assertEquals(4, response.errors().size());

                assertTrue(response.errors().stream()
                                .anyMatch(error -> error.field().equals("title")));
                assertTrue(response.errors().stream()
                                .anyMatch(error -> error.field().equals("severity")));
                assertTrue(response.errors().stream()
                                .anyMatch(error -> error.field().equals("assetId")));
                assertTrue(response.errors().stream()
                                .anyMatch(error -> error.field().equals("dueAt")));

                verifyNoInteractions(
                                organizationContext,
                                assetRepository,
                                findingService);
        }

        @Test
        void noDeberiaImportarSiElActivoNoPerteneceALaOrganizacion() {
                UUID assetId = UUID.fromString(
                                "33333333-3333-3333-3333-333333333333");

                when(organizationContext.currentOrganizationId())
                                .thenReturn(ORGANIZATION_ID);
                when(assetRepository.findByIdAndOrganizationId(
                                assetId,
                                ORGANIZATION_ID))
                                .thenReturn(Optional.empty());

                FindingCsvImportResponse response = importService.importCsv(
                                csv("Hallazgo,Descripción,HIGH," + assetId + ","));

                assertEquals(0, response.importedCount());
                assertTrue(response.findingIds().isEmpty());
                assertEquals(1, response.errors().size());
                assertEquals(2, response.errors().get(0).rowNumber());
                assertEquals("assetId", response.errors().get(0).field());

                verifyNoInteractions(findingService);
        }

        @Test
        void noDeberiaImportarSiElActivoEstaArchivado() {
                UUID assetId = UUID.fromString(
                                "44444444-4444-4444-4444-444444444444");

                AssetEntity archivedAsset = mock(AssetEntity.class);
                when(archivedAsset.getStatus()).thenReturn(AssetStatus.ARCHIVED);

                when(organizationContext.currentOrganizationId())
                                .thenReturn(ORGANIZATION_ID);
                when(assetRepository.findByIdAndOrganizationId(
                                assetId,
                                ORGANIZATION_ID))
                                .thenReturn(Optional.of(archivedAsset));

                FindingCsvImportResponse response = importService.importCsv(
                                csv("Hallazgo,Descripción,HIGH," + assetId + ","));

                assertEquals(0, response.importedCount());
                assertEquals(1, response.errors().size());
                assertEquals("assetId", response.errors().get(0).field());

                verify(findingService, never()).create(
                                anyString(),
                                anyString(),
                                any(FindingSeverity.class),
                                nullable(UUID.class),
                                nullable(Instant.class));
        }

        private byte[] csv(String row) {
                return (CSV_HEADER + row + "\r\n")
                                .getBytes(StandardCharsets.UTF_8);
        }
}