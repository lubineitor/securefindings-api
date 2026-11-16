package com.securefindings.finding.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.securefindings.asset.persistence.AssetRepository;
import com.securefindings.audit.application.AuditService;
import com.securefindings.finding.domain.Finding;
import com.securefindings.finding.domain.FindingSeverity;
import com.securefindings.finding.domain.FindingStatus;
import com.securefindings.finding.persistence.FindingEntity;
import com.securefindings.finding.persistence.FindingRepository;
import com.securefindings.security.OrganizationContext;

@ExtendWith(MockitoExtension.class)
class FindingOverdueServiceTest {

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
    void deberiaListarHallazgosVencidosDeLaOrganizacionYOrdenarlosPorFecha() {
        Instant dueAt = Instant.parse("2020-01-01T00:00:00Z");

        Finding overdueFinding = Finding.create(
                "SQL Injection",
                "Entrada de usuario sin validar",
                FindingSeverity.HIGH,
                null,
                dueAt);

        when(organizationContext.currentOrganizationId())
                .thenReturn(ORGANIZATION_ID);

        when(findingRepository.findByOrganizationIdAndDueAtLessThanEqualAndStatusIn(
                eq(ORGANIZATION_ID),
                any(Instant.class),
                eq(List.of(FindingStatus.OPEN, FindingStatus.IN_PROGRESS)),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(
                        new FindingEntity(overdueFinding, ORGANIZATION_ID))));

        Page<Finding> result = findingService.findOverduePage(0, 20);

        assertEquals(List.of(overdueFinding), result.getContent());

        ArgumentCaptor<Instant> dueAtCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        verify(findingRepository)
                .findByOrganizationIdAndDueAtLessThanEqualAndStatusIn(
                        eq(ORGANIZATION_ID),
                        dueAtCaptor.capture(),
                        eq(List.of(FindingStatus.OPEN, FindingStatus.IN_PROGRESS)),
                        pageableCaptor.capture());

        assertFalse(dueAt.isAfter(dueAtCaptor.getValue()));
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(20, pageableCaptor.getValue().getPageSize());
        assertEquals(
                Sort.by(
                        Sort.Order.asc("dueAt"),
                        Sort.Order.asc("id")),
                pageableCaptor.getValue().getSort());
    }
}