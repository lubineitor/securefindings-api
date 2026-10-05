package com.securefindings.organization.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.springframework.security.access.AccessDeniedException;

import com.securefindings.organization.domain.Organization;
import com.securefindings.organization.persistence.OrganizationEntity;
import com.securefindings.organization.persistence.OrganizationRepository;
import com.securefindings.security.OrganizationContext;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    private static final UUID ORGANIZATION_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000001");

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizationContext organizationContext;

    @InjectMocks
    private OrganizationService organizationService;

    @Test
    void deberiaDevolverLaOrganizacionDelContextoActual() {
        Organization expected = new Organization(
                ORGANIZATION_ID,
                "Secure Findings",
                "secure-findings",
                Instant.parse("2026-10-01T10:00:00Z"));

        when(organizationContext.currentOrganizationId())
                .thenReturn(ORGANIZATION_ID);

        when(organizationRepository.findById(ORGANIZATION_ID))
                .thenReturn(Optional.of(
                        new OrganizationEntity(expected)));

        Organization actual = organizationService.getCurrentOrganization();

        assertEquals(expected, actual);

        verify(organizationContext).currentOrganizationId();
        verify(organizationRepository).findById(ORGANIZATION_ID);
    }

    @Test
    void deberiaRechazarLaOrganizacionSiYaNoExiste() {
        when(organizationContext.currentOrganizationId())
                .thenReturn(ORGANIZATION_ID);

        when(organizationRepository.findById(ORGANIZATION_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                AccessDeniedException.class,
                () -> organizationService.getCurrentOrganization());

        verify(organizationRepository).findById(ORGANIZATION_ID);
    }
}