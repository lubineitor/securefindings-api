package com.securefindings.organization.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.securefindings.organization.domain.OrganizationStatusAuditAction;
import com.securefindings.organization.domain.OrganizationStatusAuditEvent;
import com.securefindings.organization.persistence.OrganizationStatusAuditEntity;
import com.securefindings.organization.persistence.OrganizationStatusAuditRepository;

@ExtendWith(MockitoExtension.class)
class OrganizationStatusAuditQueryServiceTest {

        private static final UUID ORGANIZATION_ID = UUID.fromString(
                        "00000000-0000-0000-0000-000000000001");

        private static final Instant FROM = Instant.parse("2026-11-01T00:00:00Z");

        private static final Instant TO = Instant.parse("2026-11-30T23:59:59Z");

        @Mock
        private OrganizationStatusAuditRepository auditRepository;

        private OrganizationStatusAuditQueryService queryService;

        @BeforeEach
        void crearServicio() {
                queryService = new OrganizationStatusAuditQueryService(auditRepository);
        }

        @Test
        void aplicaLosFiltrosYOrdenaLosEventosMasRecientesPrimero() {
                OrganizationStatusAuditEvent event = new OrganizationStatusAuditEvent(
                                UUID.fromString("00000000-0000-0000-0000-000000000401"),
                                ORGANIZATION_ID,
                                OrganizationStatusAuditAction.SUSPENDED,
                                "admin",
                                Instant.parse("2026-11-10T10:00:00Z"),
                                "request-401");

                OrganizationStatusAuditEntity entity = new OrganizationStatusAuditEntity(event);

                Page<OrganizationStatusAuditEntity> expected = new PageImpl<>(
                                List.of(entity),
                                PageRequest.of(0, 20),
                                1);

                when(auditRepository.findPageByOrganizationIdAndFilters(
                                eq(ORGANIZATION_ID),
                                eq(OrganizationStatusAuditAction.SUSPENDED),
                                eq("request-401"),
                                eq("admin"),
                                eq(FROM),
                                eq(TO),
                                any(Pageable.class)))
                                .thenReturn(expected);

                Page<OrganizationStatusAuditEntity> actual = queryService.findPageByOrganizationId(
                                ORGANIZATION_ID,
                                0,
                                20,
                                OrganizationStatusAuditAction.SUSPENDED,
                                "request-401",
                                "admin",
                                FROM,
                                TO);

                assertEquals(1, actual.getTotalElements());
                assertEquals(entity, actual.getContent().get(0));

                ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

                verify(auditRepository).findPageByOrganizationIdAndFilters(
                                eq(ORGANIZATION_ID),
                                eq(OrganizationStatusAuditAction.SUSPENDED),
                                eq("request-401"),
                                eq("admin"),
                                eq(FROM),
                                eq(TO),
                                pageableCaptor.capture());

                assertEquals(0, pageableCaptor.getValue().getPageNumber());
                assertEquals(20, pageableCaptor.getValue().getPageSize());
                assertEquals(
                                Sort.by(
                                                Sort.Order.desc("occurredAt"),
                                                Sort.Order.desc("id")),
                                pageableCaptor.getValue().getSort());
        }

        @Test
        void rechazaUnRangoDeFechasInvertidoSinConsultarElRepositorio() {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> queryService.findPageByOrganizationId(
                                                ORGANIZATION_ID,
                                                0,
                                                20,
                                                null,
                                                null,
                                                null,
                                                TO,
                                                FROM));

                verifyNoInteractions(auditRepository);
        }

        @Test
        void exportaEventosAplicandoFiltrosYLimitaLaConsultaAUnEventoExtra() {
                OrganizationStatusAuditEvent event = new OrganizationStatusAuditEvent(
                                UUID.fromString("00000000-0000-0000-0000-000000000401"),
                                ORGANIZATION_ID,
                                OrganizationStatusAuditAction.SUSPENDED,
                                "admin",
                                Instant.parse("2026-11-10T10:00:00Z"),
                                "request-401");

                OrganizationStatusAuditEntity entity = new OrganizationStatusAuditEntity(event);

                when(auditRepository.findPageByOrganizationIdAndFilters(
                                eq(ORGANIZATION_ID),
                                eq(OrganizationStatusAuditAction.SUSPENDED),
                                eq("request-401"),
                                eq("admin"),
                                eq(FROM),
                                eq(TO),
                                any(Pageable.class)))
                                .thenReturn(new PageImpl<>(
                                                List.of(entity),
                                                PageRequest.of(
                                                                0,
                                                                OrganizationStatusAuditQueryService.MAX_EXPORT_ROWS
                                                                                + 1),
                                                1));

                List<OrganizationStatusAuditEntity> actual = queryService.findForExport(
                                ORGANIZATION_ID,
                                OrganizationStatusAuditAction.SUSPENDED,
                                "request-401",
                                "admin",
                                FROM,
                                TO);

                assertEquals(List.of(entity), actual);

                ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

                verify(auditRepository).findPageByOrganizationIdAndFilters(
                                eq(ORGANIZATION_ID),
                                eq(OrganizationStatusAuditAction.SUSPENDED),
                                eq("request-401"),
                                eq("admin"),
                                eq(FROM),
                                eq(TO),
                                pageableCaptor.capture());

                assertEquals(0, pageableCaptor.getValue().getPageNumber());
                assertEquals(
                                OrganizationStatusAuditQueryService.MAX_EXPORT_ROWS + 1,
                                pageableCaptor.getValue().getPageSize());
                assertEquals(
                                Sort.by(
                                                Sort.Order.asc("occurredAt"),
                                                Sort.Order.asc("id")),
                                pageableCaptor.getValue().getSort());
        }

        @Test
        void rechazaLaExportacionSiHayMasDeMilEventos() {
                OrganizationStatusAuditEvent event = new OrganizationStatusAuditEvent(
                                UUID.fromString("00000000-0000-0000-0000-000000000401"),
                                ORGANIZATION_ID,
                                OrganizationStatusAuditAction.SUSPENDED,
                                "admin",
                                Instant.parse("2026-11-10T10:00:00Z"),
                                "request-401");

                OrganizationStatusAuditEntity entity = new OrganizationStatusAuditEntity(event);

                when(auditRepository.findPageByOrganizationIdAndFilters(
                                eq(ORGANIZATION_ID),
                                eq(null),
                                eq(null),
                                eq(null),
                                eq(null),
                                eq(null),
                                any(Pageable.class)))
                                .thenReturn(new PageImpl<>(
                                                List.of(entity),
                                                PageRequest.of(
                                                                0,
                                                                OrganizationStatusAuditQueryService.MAX_EXPORT_ROWS
                                                                                + 1),
                                                OrganizationStatusAuditQueryService.MAX_EXPORT_ROWS + 1L));

                assertThrows(
                                OrganizationStatusAuditExportLimitExceededException.class,
                                () -> queryService.findForExport(
                                                ORGANIZATION_ID,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null));
        }
}