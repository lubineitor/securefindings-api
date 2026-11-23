package com.securefindings.organization.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.springframework.data.domain.Sort;

import com.securefindings.organization.domain.OrganizationStatusAuditAction;
import com.securefindings.organization.domain.OrganizationStatusAuditEvent;
import com.securefindings.organization.persistence.OrganizationStatusAuditEntity;
import com.securefindings.organization.persistence.OrganizationStatusAuditRepository;

@ExtendWith(MockitoExtension.class)
class OrganizationStatusAuditQueryServiceTest {

    private static final UUID ORGANIZATION_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000001");

    @Mock
    private OrganizationStatusAuditRepository auditRepository;

    @InjectMocks
    private OrganizationStatusAuditQueryService queryService;

    @Test
    void devuelveLaPaginaOrdenadaPorFechaEIdentificadorDescendentes() {
        OrganizationStatusAuditEvent event = new OrganizationStatusAuditEvent(
                UUID.fromString("00000000-0000-0000-0000-000000000401"),
                ORGANIZATION_ID,
                OrganizationStatusAuditAction.SUSPENDED,
                "admin",
                Instant.parse("2026-11-23T10:00:00Z"),
                "request-401");

        OrganizationStatusAuditEntity entity = new OrganizationStatusAuditEntity(event);

        Page<OrganizationStatusAuditEntity> expected = new PageImpl<>(
                List.of(entity),
                PageRequest.of(0, 20),
                1);

        when(auditRepository.findByOrganizationId(
                eq(ORGANIZATION_ID),
                any(Pageable.class)))
                .thenReturn(expected);

        Page<OrganizationStatusAuditEntity> actual = queryService.findPageByOrganizationId(
                ORGANIZATION_ID,
                0,
                20);

        assertEquals(1, actual.getTotalElements());
        assertEquals(entity, actual.getContent().get(0));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        verify(auditRepository).findByOrganizationId(
                eq(ORGANIZATION_ID),
                pageableCaptor.capture());

        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(20, pageableCaptor.getValue().getPageSize());
        assertEquals(
                Sort.by(
                        Sort.Order.desc("occurredAt"),
                        Sort.Order.desc("id")),
                pageableCaptor.getValue().getSort());
    }
}