package com.securefindings.organization.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.securefindings.organization.domain.Organization;
import com.securefindings.organization.domain.OrganizationStatus;
import com.securefindings.organization.persistence.OrganizationEntity;
import com.securefindings.organization.persistence.OrganizationRepository;

@ExtendWith(MockitoExtension.class)
class OrganizationLifecycleServiceTest {

    private static final UUID ORGANIZATION_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000001");

    private static final Instant CREATED_AT = Instant.parse(
            "2026-10-01T10:00:00Z");

    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private OrganizationLifecycleService lifecycleService;

    @Test
    void deberiaSuspenderUnaOrganizacionActiva() {
        Organization active = organization(OrganizationStatus.ACTIVE);

        when(organizationRepository.findById(ORGANIZATION_ID))
                .thenReturn(Optional.of(new OrganizationEntity(active)));
        when(organizationRepository.save(any(OrganizationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Organization result = lifecycleService.changeStatus(
                ORGANIZATION_ID,
                OrganizationStatus.SUSPENDED);

        assertEquals(OrganizationStatus.SUSPENDED, result.status());
        verify(organizationRepository).save(any(OrganizationEntity.class));
    }

    @Test
    void deberiaReactivarUnaOrganizacionSuspendida() {
        Organization suspended = organization(OrganizationStatus.SUSPENDED);

        when(organizationRepository.findById(ORGANIZATION_ID))
                .thenReturn(Optional.of(new OrganizationEntity(suspended)));
        when(organizationRepository.save(any(OrganizationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Organization result = lifecycleService.changeStatus(
                ORGANIZATION_ID,
                OrganizationStatus.ACTIVE);

        assertEquals(OrganizationStatus.ACTIVE, result.status());
        verify(organizationRepository).save(any(OrganizationEntity.class));
    }

    @Test
    void noDeberiaGuardarSiElEstadoYaEsElSolicitado() {
        Organization active = organization(OrganizationStatus.ACTIVE);

        when(organizationRepository.findById(ORGANIZATION_ID))
                .thenReturn(Optional.of(new OrganizationEntity(active)));

        Organization result = lifecycleService.changeStatus(
                ORGANIZATION_ID,
                OrganizationStatus.ACTIVE);

        assertEquals(OrganizationStatus.ACTIVE, result.status());
        verify(organizationRepository, never()).save(any(OrganizationEntity.class));
    }

    @Test
    void deberiaRechazarUnaOrganizacionInexistente() {
        when(organizationRepository.findById(ORGANIZATION_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                OrganizationNotFoundException.class,
                () -> lifecycleService.changeStatus(
                        ORGANIZATION_ID,
                        OrganizationStatus.SUSPENDED));

        verify(organizationRepository, never()).save(any(OrganizationEntity.class));
    }

    private Organization organization(OrganizationStatus status) {
        return new Organization(
                ORGANIZATION_ID,
                "Secure Findings",
                "secure-findings",
                CREATED_AT,
                status);
    }
}