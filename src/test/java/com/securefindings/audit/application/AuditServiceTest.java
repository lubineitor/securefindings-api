package com.securefindings.audit.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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

        @Test
        void deberiaPaginarLaAuditoriaFiltrandoPorOrganizacion() {
                UUID findingId = UUID.randomUUID();
                UUID organizationId = UUID.fromString(
                                "00000000-0000-0000-0000-000000000001");

                FindingAuditEntity firstEvent = new FindingAuditEntity(
                                UUID.randomUUID(),
                                findingId,
                                organizationId,
                                AuditAction.CREATED,
                                "analista",
                                Instant.parse("2026-09-06T08:00:00Z"));

                Page<FindingAuditEntity> auditPage = new PageImpl<>(
                                List.of(firstEvent),
                                PageRequest.of(0, 1),
                                2);

                when(organizationContext.currentOrganizationId())
                                .thenReturn(organizationId);

                when(auditRepository
                                .findByFindingIdAndOrganizationIdOrderByOccurredAtAsc(
                                                eq(findingId),
                                                eq(organizationId),
                                                any(Pageable.class)))
                                .thenReturn(auditPage);

                Page<FindingAuditEntity> result = auditService
                                .findPageByFindingId(findingId, 0, 1);

                assertEquals(1, result.getContent().size());
                assertEquals(2, result.getTotalElements());
                assertEquals(2, result.getTotalPages());
                assertEquals(AuditAction.CREATED,
                                result.getContent().get(0).getAction());

                verify(organizationContext).currentOrganizationId();
                verify(auditRepository)
                                .findByFindingIdAndOrganizationIdOrderByOccurredAtAsc(
                                                eq(findingId),
                                                eq(organizationId),
                                                any(Pageable.class));
        }
}