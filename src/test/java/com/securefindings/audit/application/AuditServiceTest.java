package com.securefindings.audit.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.securefindings.audit.domain.AuditAction;
import com.securefindings.audit.persistence.FindingAuditEntity;
import com.securefindings.audit.persistence.FindingAuditRepository;
import com.securefindings.finding.application.FindingNotFoundException;
import com.securefindings.finding.persistence.FindingEntity;
import com.securefindings.finding.persistence.FindingRepository;
import com.securefindings.security.OrganizationContext;
import com.securefindings.security.RequestCorrelationFilter;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

        private static final UUID ORGANIZATION_ID = UUID.fromString(
                        "00000000-0000-0000-0000-000000000001");

        @Mock
        private FindingAuditRepository auditRepository;

        @Mock
        private FindingRepository findingRepository;

        @Mock
        private OrganizationContext organizationContext;

        @InjectMocks
        private AuditService auditService;

        @Test
        void deberiaRegistrarLaAuditoriaEnLaOrganizacionActual() {
                UUID findingId = UUID.randomUUID();

                when(organizationContext.currentOrganizationId())
                                .thenReturn(ORGANIZATION_ID);

                stubFindingExists(findingId, ORGANIZATION_ID);

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

                stubFindingExists(findingId, ORGANIZATION_ID);

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

                stubFindingExists(findingId, organizationId);

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

        @Test
        void deberiaFiltrarLaAuditoriaSoloPorAccion() {
                UUID findingId = UUID.randomUUID();

                FindingAuditEntity event = crearEvento(
                                findingId,
                                AuditAction.UPDATED,
                                "audit-request-123");

                Page<FindingAuditEntity> auditPage = new PageImpl<>(
                                List.of(event),
                                PageRequest.of(0, 20),
                                1);

                when(organizationContext.currentOrganizationId())
                                .thenReturn(ORGANIZATION_ID);

                stubFindingExists(findingId, ORGANIZATION_ID);

                when(auditRepository
                                .findByFindingIdAndOrganizationIdAndActionOrderByOccurredAtAsc(
                                                eq(findingId),
                                                eq(ORGANIZATION_ID),
                                                eq(AuditAction.UPDATED),
                                                any(Pageable.class)))
                                .thenReturn(auditPage);

                Page<FindingAuditEntity> result = auditService.findPageByFindingId(
                                findingId,
                                0,
                                20,
                                AuditAction.UPDATED,
                                null);

                assertEquals(1, result.getTotalElements());
                assertEquals(
                                AuditAction.UPDATED,
                                result.getContent().get(0).getAction());

                verify(auditRepository)
                                .findByFindingIdAndOrganizationIdAndActionOrderByOccurredAtAsc(
                                                eq(findingId),
                                                eq(ORGANIZATION_ID),
                                                eq(AuditAction.UPDATED),
                                                any(Pageable.class));
        }

        @Test
        void deberiaFiltrarLaAuditoriaSoloPorRequestId() {
                UUID findingId = UUID.randomUUID();

                FindingAuditEntity event = crearEvento(
                                findingId,
                                AuditAction.CREATED,
                                "audit-request-123");

                Page<FindingAuditEntity> auditPage = new PageImpl<>(
                                List.of(event),
                                PageRequest.of(0, 20),
                                1);

                when(organizationContext.currentOrganizationId())
                                .thenReturn(ORGANIZATION_ID);

                stubFindingExists(findingId, ORGANIZATION_ID);

                when(auditRepository
                                .findByFindingIdAndOrganizationIdAndRequestIdOrderByOccurredAtAsc(
                                                eq(findingId),
                                                eq(ORGANIZATION_ID),
                                                eq("audit-request-123"),
                                                any(Pageable.class)))
                                .thenReturn(auditPage);

                Page<FindingAuditEntity> result = auditService.findPageByFindingId(
                                findingId,
                                0,
                                20,
                                null,
                                "audit-request-123");

                assertEquals(1, result.getTotalElements());
                assertEquals(
                                "audit-request-123",
                                result.getContent().get(0).getRequestId());

                verify(auditRepository)
                                .findByFindingIdAndOrganizationIdAndRequestIdOrderByOccurredAtAsc(
                                                eq(findingId),
                                                eq(ORGANIZATION_ID),
                                                eq("audit-request-123"),
                                                any(Pageable.class));
        }

        @Test
        void deberiaFiltrarLaAuditoriaPorAccionYRequestId() {
                UUID findingId = UUID.randomUUID();

                FindingAuditEntity event = crearEvento(
                                findingId,
                                AuditAction.UPDATED,
                                "audit-request-123");

                Page<FindingAuditEntity> auditPage = new PageImpl<>(
                                List.of(event),
                                PageRequest.of(0, 20),
                                1);

                when(organizationContext.currentOrganizationId())
                                .thenReturn(ORGANIZATION_ID);

                stubFindingExists(findingId, ORGANIZATION_ID);

                when(auditRepository
                                .findByFindingIdAndOrganizationIdAndActionAndRequestIdOrderByOccurredAtAsc(
                                                eq(findingId),
                                                eq(ORGANIZATION_ID),
                                                eq(AuditAction.UPDATED),
                                                eq("audit-request-123"),
                                                any(Pageable.class)))
                                .thenReturn(auditPage);

                Page<FindingAuditEntity> result = auditService.findPageByFindingId(
                                findingId,
                                0,
                                20,
                                AuditAction.UPDATED,
                                "audit-request-123");

                assertEquals(1, result.getTotalElements());
                assertEquals(
                                AuditAction.UPDATED,
                                result.getContent().get(0).getAction());
                assertEquals(
                                "audit-request-123",
                                result.getContent().get(0).getRequestId());

                verify(auditRepository)
                                .findByFindingIdAndOrganizationIdAndActionAndRequestIdOrderByOccurredAtAsc(
                                                eq(findingId),
                                                eq(ORGANIZATION_ID),
                                                eq(AuditAction.UPDATED),
                                                eq("audit-request-123"),
                                                any(Pageable.class));
        }

        @Test
        void deberiaRechazarLaAuditoriaDeUnHallazgoInexistente() {
                UUID findingId = UUID.randomUUID();

                when(organizationContext.currentOrganizationId())
                                .thenReturn(ORGANIZATION_ID);

                when(findingRepository.findByIdAndOrganizationId(
                                findingId,
                                ORGANIZATION_ID))
                                .thenReturn(Optional.empty());

                when(auditRepository.existsByFindingIdAndOrganizationId(
                                findingId,
                                ORGANIZATION_ID))
                                .thenReturn(false);

                assertThrows(
                                FindingNotFoundException.class,
                                () -> auditService.findPageByFindingId(
                                                findingId,
                                                0,
                                                20));

                verify(auditRepository)
                                .existsByFindingIdAndOrganizationId(
                                                findingId,
                                                ORGANIZATION_ID);
        }

        @Test
        void deberiaConsultarLaAuditoriaDeUnHallazgoEliminado() {
                UUID findingId = UUID.randomUUID();

                FindingAuditEntity deletedEvent = crearEvento(
                                findingId,
                                AuditAction.DELETED,
                                "audit-request-123");

                Page<FindingAuditEntity> auditPage = new PageImpl<>(
                                List.of(deletedEvent),
                                PageRequest.of(0, 20),
                                1);

                when(organizationContext.currentOrganizationId())
                                .thenReturn(ORGANIZATION_ID);

                when(findingRepository.findByIdAndOrganizationId(
                                findingId,
                                ORGANIZATION_ID))
                                .thenReturn(Optional.empty());

                when(auditRepository.existsByFindingIdAndOrganizationId(
                                findingId,
                                ORGANIZATION_ID))
                                .thenReturn(true);

                when(auditRepository
                                .findByFindingIdAndOrganizationIdOrderByOccurredAtAsc(
                                                eq(findingId),
                                                eq(ORGANIZATION_ID),
                                                any(Pageable.class)))
                                .thenReturn(auditPage);

                Page<FindingAuditEntity> result = auditService.findPageByFindingId(
                                findingId,
                                0,
                                20);

                assertEquals(1, result.getTotalElements());
                assertEquals(
                                AuditAction.DELETED,
                                result.getContent().get(0).getAction());
        }

        @Test
        void deberiaPersistirElRequestIdActual() {
                UUID findingId = UUID.randomUUID();

                when(organizationContext.currentOrganizationId())
                                .thenReturn(ORGANIZATION_ID);

                stubFindingExists(findingId, ORGANIZATION_ID);

                MDC.put(
                                RequestCorrelationFilter.MDC_KEY,
                                "audit-request-123");

                try {
                        auditService.register(
                                        findingId,
                                        AuditAction.CREATED,
                                        "analista");
                } finally {
                        MDC.remove(RequestCorrelationFilter.MDC_KEY);
                }

                ArgumentCaptor<FindingAuditEntity> captor = ArgumentCaptor.forClass(
                                FindingAuditEntity.class);

                verify(auditRepository).save(captor.capture());

                assertEquals(
                                "audit-request-123",
                                captor.getValue().getRequestId());
        }

        @Test
        void deberiaRechazarRegistrarAuditoriaDeUnHallazgoDeOtraOrganizacion() {
                UUID findingId = UUID.randomUUID();

                when(organizationContext.currentOrganizationId())
                                .thenReturn(ORGANIZATION_ID);

                when(findingRepository.findByIdAndOrganizationId(
                                findingId,
                                ORGANIZATION_ID))
                                .thenReturn(Optional.empty());

                assertThrows(
                                FindingNotFoundException.class,
                                () -> auditService.register(
                                                findingId,
                                                AuditAction.UPDATED,
                                                "analista"));

                verify(auditRepository, never()).save(any(FindingAuditEntity.class));
        }

        @Test
        void deberiaPermitirRegistrarLaEliminacionDeUnHallazgoYaEliminado() {
                UUID findingId = UUID.randomUUID();

                when(organizationContext.currentOrganizationId())
                                .thenReturn(ORGANIZATION_ID);

                when(findingRepository.findByIdAndOrganizationId(
                                findingId,
                                ORGANIZATION_ID))
                                .thenReturn(Optional.empty());

                when(auditRepository.existsByFindingIdAndOrganizationId(
                                findingId,
                                ORGANIZATION_ID))
                                .thenReturn(true);

                auditService.register(
                                findingId,
                                AuditAction.DELETED,
                                "analista");

                verify(auditRepository).save(any(FindingAuditEntity.class));
        }

        private FindingAuditEntity crearEvento(
                        UUID findingId,
                        AuditAction action,
                        String requestId) {

                return new FindingAuditEntity(
                                UUID.randomUUID(),
                                findingId,
                                ORGANIZATION_ID,
                                action,
                                "analista",
                                Instant.parse("2026-09-06T08:05:00Z"),
                                requestId);
        }

        private void stubFindingExists(
                        UUID findingId,
                        UUID organizationId) {

                when(findingRepository.findByIdAndOrganizationId(
                                findingId,
                                organizationId))
                                .thenReturn(Optional.of(mock(FindingEntity.class)));
        }
}
