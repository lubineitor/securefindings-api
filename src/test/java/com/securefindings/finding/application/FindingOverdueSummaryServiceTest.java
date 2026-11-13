package com.securefindings.finding.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.securefindings.asset.persistence.AssetRepository;
import com.securefindings.audit.application.AuditService;
import com.securefindings.finding.domain.FindingOverdueSummary;
import com.securefindings.finding.domain.FindingSeverity;
import com.securefindings.finding.domain.FindingStatus;
import com.securefindings.finding.persistence.FindingRepository;
import com.securefindings.security.OrganizationContext;

@ExtendWith(MockitoExtension.class)
class FindingOverdueSummaryServiceTest {

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

    @InjectMocks
    private FindingService findingService;

    @Test
    void deberiaAgruparHallazgosVencidosPorEstadoYSeveridad() {
        FindingRepository.OverdueCountProjection openHigh = mock(
                FindingRepository.OverdueCountProjection.class);

        when(openHigh.getStatus()).thenReturn(FindingStatus.OPEN);
        when(openHigh.getSeverity()).thenReturn(FindingSeverity.HIGH);
        when(openHigh.getTotal()).thenReturn(1L);

        FindingRepository.OverdueCountProjection inProgressMedium = mock(
                FindingRepository.OverdueCountProjection.class);

        when(inProgressMedium.getStatus())
                .thenReturn(FindingStatus.IN_PROGRESS);
        when(inProgressMedium.getSeverity())
                .thenReturn(FindingSeverity.MEDIUM);
        when(inProgressMedium.getTotal()).thenReturn(2L);

        when(organizationContext.currentOrganizationId())
                .thenReturn(ORGANIZATION_ID);

        when(findingRepository.countOverdueByOrganizationAndStatuses(
                eq(ORGANIZATION_ID),
                any(Instant.class),
                eq(List.of(FindingStatus.OPEN, FindingStatus.IN_PROGRESS))))
                .thenReturn(List.of(openHigh, inProgressMedium));

        FindingOverdueSummary summary = findingService.getOverdueSummary();

        assertNotNull(summary.asOf());
        assertEquals(3L, summary.total());

        assertEquals(1L, summary.byStatus().get(FindingStatus.OPEN));
        assertEquals(2L, summary.byStatus().get(FindingStatus.IN_PROGRESS));
        assertEquals(0L, summary.byStatus().get(FindingStatus.RESOLVED));
        assertEquals(0L, summary.byStatus().get(FindingStatus.FALSE_POSITIVE));

        assertEquals(1L, summary.bySeverity().get(FindingSeverity.HIGH));
        assertEquals(2L, summary.bySeverity().get(FindingSeverity.MEDIUM));
        assertEquals(0L, summary.bySeverity().get(FindingSeverity.LOW));
        assertEquals(0L, summary.bySeverity().get(FindingSeverity.CRITICAL));

        verify(findingRepository).countOverdueByOrganizationAndStatuses(
                eq(ORGANIZATION_ID),
                any(Instant.class),
                eq(List.of(FindingStatus.OPEN, FindingStatus.IN_PROGRESS)));
    }

    @Test
    void deberiaDevolverRecuentosCeroSiNoHayHallazgosVencidos() {
        when(organizationContext.currentOrganizationId())
                .thenReturn(ORGANIZATION_ID);

        when(findingRepository.countOverdueByOrganizationAndStatuses(
                eq(ORGANIZATION_ID),
                any(Instant.class),
                eq(List.of(FindingStatus.OPEN, FindingStatus.IN_PROGRESS))))
                .thenReturn(List.of());

        FindingOverdueSummary summary = findingService.getOverdueSummary();

        assertEquals(0L, summary.total());

        for (FindingStatus status : FindingStatus.values()) {
            assertEquals(0L, summary.byStatus().get(status));
        }

        for (FindingSeverity severity : FindingSeverity.values()) {
            assertEquals(0L, summary.bySeverity().get(severity));
        }
    }
}