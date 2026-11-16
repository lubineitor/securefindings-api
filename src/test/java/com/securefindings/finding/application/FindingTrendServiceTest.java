package com.securefindings.finding.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.securefindings.asset.persistence.AssetRepository;
import com.securefindings.audit.application.AuditService;
import com.securefindings.finding.domain.FindingSeverity;
import com.securefindings.finding.domain.FindingTrend;
import com.securefindings.finding.domain.FindingTrendInterval;
import com.securefindings.finding.persistence.FindingRepository;
import com.securefindings.security.OrganizationContext;

@ExtendWith(MockitoExtension.class)
class FindingTrendServiceTest {

        private static final UUID ORGANIZATION_ID = UUID.fromString(
                        "00000000-0000-0000-0000-000000000001");

        @Mock
        private FindingRepository findingRepository;

        @Mock
        private AssetRepository assetRepository;

        @Mock
        private AuditService auditService;

        @Mock
        private OrganizationContext organizationContext;

        private FindingService findingService;

        @BeforeEach
        void setUp() {
                findingService = new FindingService(
                                findingRepository,
                                assetRepository,
                                auditService,
                                organizationContext);
        }

        @Test
        void deberiaAgruparPorSeveridadYRellenarPeriodosSinResultados() {
                Instant from = Instant.parse("2026-01-01T12:00:00Z");
                Instant to = Instant.parse("2026-01-04T00:00:00Z");

                when(organizationContext.currentOrganizationId())
                                .thenReturn(ORGANIZATION_ID);

                when(findingRepository.findTrendByOrganization(
                                ORGANIZATION_ID,
                                from,
                                to,
                                "day"))
                                .thenReturn(List.of(
                                                new TrendProjectionRow(
                                                                LocalDate.parse("2026-01-01"),
                                                                FindingSeverity.HIGH.name(),
                                                                1L),
                                                new TrendProjectionRow(
                                                                LocalDate.parse("2026-01-01"),
                                                                FindingSeverity.LOW.name(),
                                                                1L),
                                                new TrendProjectionRow(
                                                                LocalDate.parse("2026-01-03"),
                                                                FindingSeverity.CRITICAL.name(),
                                                                1L)));

                FindingTrend trend = findingService.getTrend(
                                from,
                                to,
                                FindingTrendInterval.DAY,
                                null);

                assertEquals(3, trend.points().size());

                FindingTrend.Point firstDay = trend.points().get(0);
                assertEquals(
                                Instant.parse("2026-01-01T00:00:00Z"),
                                firstDay.bucketStart());
                assertEquals(2L, firstDay.total());
                assertEquals(
                                1L,
                                firstDay.bySeverity().get(FindingSeverity.HIGH));
                assertEquals(
                                1L,
                                firstDay.bySeverity().get(FindingSeverity.LOW));

                FindingTrend.Point emptyDay = trend.points().get(1);
                assertEquals(
                                Instant.parse("2026-01-02T00:00:00Z"),
                                emptyDay.bucketStart());
                assertEquals(0L, emptyDay.total());
                assertEquals(
                                0L,
                                emptyDay.bySeverity().get(FindingSeverity.CRITICAL));

                FindingTrend.Point thirdDay = trend.points().get(2);
                assertEquals(
                                Instant.parse("2026-01-03T00:00:00Z"),
                                thirdDay.bucketStart());
                assertEquals(1L, thirdDay.total());
                assertEquals(
                                1L,
                                thirdDay.bySeverity().get(FindingSeverity.CRITICAL));

                verify(findingRepository).findTrendByOrganization(
                                ORGANIZATION_ID,
                                from,
                                to,
                                "day");
        }

        @Test
        void deberiaAplicarElFiltroPorActivo() {
                Instant from = Instant.parse("2026-01-01T00:00:00Z");
                Instant to = Instant.parse("2026-02-01T00:00:00Z");
                UUID assetId = UUID.randomUUID();

                when(organizationContext.currentOrganizationId())
                                .thenReturn(ORGANIZATION_ID);

                when(findingRepository.findTrendByOrganizationAndAssetId(
                                ORGANIZATION_ID,
                                assetId,
                                from,
                                to,
                                "month"))
                                .thenReturn(List.of());

                FindingTrend trend = findingService.getTrend(
                                from,
                                to,
                                FindingTrendInterval.MONTH,
                                assetId);

                assertEquals(1, trend.points().size());
                assertEquals(0L, trend.points().get(0).total());

                verify(findingRepository).findTrendByOrganizationAndAssetId(
                                ORGANIZATION_ID,
                                assetId,
                                from,
                                to,
                                "month");
        }

        @Test
        void deberiaRechazarRangoVacioInvertidoOMayorA366Dias() {
                Instant from = Instant.parse("2026-01-01T00:00:00Z");

                assertThrows(
                                IllegalArgumentException.class,
                                () -> findingService.getTrend(
                                                from,
                                                from,
                                                FindingTrendInterval.DAY,
                                                null));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> findingService.getTrend(
                                                from,
                                                from.plus(Duration.ofDays(366)).plusSeconds(1),
                                                FindingTrendInterval.DAY,
                                                null));

                verifyNoInteractions(findingRepository, organizationContext);
        }

        private record TrendProjectionRow(
                        LocalDate bucketDate,
                        String severity,
                        Long total)
                        implements FindingRepository.TrendCountProjection {

                @Override
                public LocalDate getBucketDate() {
                        return bucketDate;
                }

                @Override
                public String getSeverity() {
                        return severity;
                }

                @Override
                public Long getTotal() {
                        return total;
                }
        }
}