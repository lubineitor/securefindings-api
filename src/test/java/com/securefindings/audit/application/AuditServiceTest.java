package com.securefindings.audit.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

import com.securefindings.audit.domain.AuditAction;
import com.securefindings.audit.persistence.FindingAuditEntity;
import com.securefindings.audit.persistence.FindingAuditRepository;
import com.securefindings.security.OrganizationContext;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    private static final UUID ORGANIZATION_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000001");

    @Mock
    private FindingAuditRepository auditRepository;

    @Mock
    private OrganizationContext organizationContext;

    @InjectMocks
    private AuditService auditService;

    @Test
    void deberiaRegistrarLaAuditoriaEnLaOrganizacionActual() {
        UUID findingId = UUID.randomUUID();

        when(organizationContext.currentOrganizationId())
                .thenReturn(ORGANIZATION_ID);

        auditService.register(
                findingId,
                AuditAction.CREATED,
                "analista");

        ArgumentCaptor<FindingAuditEntity> captor = ArgumentCaptor.forClass(FindingAuditEntity.class);

        verify(auditRepository).save(captor.capture());

        FindingAuditEntity savedEvent = captor.getValue();

        assertNotNull(savedEvent.getId());
        assertEquals(findingId, savedEvent.getFindingId());
        assertEquals(
                ORGANIZATION_ID,
                savedEvent.getOrganizationId());
        assertEquals(
                AuditAction.CREATED,
                savedEvent.getAction());
        assertEquals("analista", savedEvent.getActor());
        assertNotNull(savedEvent.getOccurredAt());
    }

    @Test
    void deberiaConsultarLaAuditoriaFiltrandoPorOrganizacion() {
        UUID findingId = UUID.randomUUID();

        FindingAuditEntity event = new FindingAuditEntity(
                UUID.randomUUID(),
                findingId,
                ORGANIZATION_ID,
                AuditAction.CREATED,
                "analista",
                Instant.now());

        when(organizationContext.currentOrganizationId())
                .thenReturn(ORGANIZATION_ID);

        when(auditRepository
                .findByFindingIdAndOrganizationIdOrderByOccurredAtAsc(
                        findingId,
                        ORGANIZATION_ID))
                .thenReturn(List.of(event));

        List<FindingAuditEntity> events = auditService.findByFindingId(findingId);

        assertEquals(List.of(event), events);

        verify(auditRepository)
                .findByFindingIdAndOrganizationIdOrderByOccurredAtAsc(
                        findingId,
                        ORGANIZATION_ID);
    }
}