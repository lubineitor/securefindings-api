package com.securefindings.audit.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.securefindings.audit.domain.AuditAction;
import com.securefindings.audit.persistence.FindingAuditEntity;
import com.securefindings.audit.persistence.FindingAuditRepository;
import com.securefindings.finding.domain.FindingStatus;
import com.securefindings.finding.persistence.FindingEntity;
import com.securefindings.finding.persistence.FindingRepository;
import com.securefindings.security.OrganizationContext;

@ExtendWith(MockitoExtension.class)
class AuditStatusChangeServiceTest {

    private static final UUID ORGANIZATION_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000001");

    private static final UUID FINDING_ID = UUID.fromString(
            "11111111-1111-1111-1111-111111111111");

    @Mock
    private FindingAuditRepository auditRepository;

    @Mock
    private FindingRepository findingRepository;

    @Mock
    private OrganizationContext organizationContext;

    @Test
    void deberiaGuardarLosEstadosAnteriorYNuevo() {
        when(organizationContext.currentOrganizationId())
                .thenReturn(ORGANIZATION_ID);

        when(findingRepository.findByIdAndOrganizationId(
                FINDING_ID,
                ORGANIZATION_ID))
                .thenReturn(Optional.of(mock(FindingEntity.class)));

        AuditService auditService = new AuditService(
                auditRepository,
                findingRepository,
                organizationContext);

        auditService.registerStatusChange(
                FINDING_ID,
                FindingStatus.OPEN,
                FindingStatus.IN_PROGRESS,
                "analista");

        ArgumentCaptor<FindingAuditEntity> captor = ArgumentCaptor.forClass(FindingAuditEntity.class);

        verify(auditRepository).save(captor.capture());

        FindingAuditEntity event = captor.getValue();

        assertEquals(FINDING_ID, event.getFindingId());
        assertEquals(ORGANIZATION_ID, event.getOrganizationId());
        assertEquals(AuditAction.STATUS_CHANGED, event.getAction());
        assertEquals("analista", event.getActor());
        assertEquals(FindingStatus.OPEN, event.getPreviousStatus());
        assertEquals(FindingStatus.IN_PROGRESS, event.getNewStatus());
    }
}