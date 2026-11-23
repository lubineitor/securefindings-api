package com.securefindings.organization.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.securefindings.organization.domain.Organization;
import com.securefindings.organization.domain.OrganizationStatus;
import com.securefindings.organization.domain.OrganizationStatusAuditAction;
import com.securefindings.organization.persistence.OrganizationEntity;
import com.securefindings.organization.persistence.OrganizationRepository;
import com.securefindings.organization.persistence.OrganizationStatusAuditEntity;
import com.securefindings.organization.persistence.OrganizationStatusAuditRepository;
import com.securefindings.security.RequestCorrelationFilter;

@ExtendWith(MockitoExtension.class)
class OrganizationLifecycleServiceTest {

        private static final UUID ORGANIZATION_ID = UUID.fromString(
                        "00000000-0000-0000-0000-000000000001");

        private static final Instant CREATED_AT = Instant.parse(
                        "2026-10-01T10:00:00Z");

        private static final String SUSPENSION_REASON = "Incidente confirmado por revisión";

        private static final String REACTIVATION_REASON = "Remediación validada por el administrador";

        @Mock
        private OrganizationRepository organizationRepository;

        @Mock
        private OrganizationStatusAuditRepository auditRepository;

        private OrganizationLifecycleService lifecycleService;

        @BeforeEach
        void crearServicio() {
                lifecycleService = new OrganizationLifecycleService(
                                organizationRepository,
                                auditRepository);
        }

        @AfterEach
        void limpiarContextos() {
                SecurityContextHolder.clearContext();
                MDC.clear();
        }

        @Test
        void deberiaSuspenderUnaOrganizacionActivaYRegistrarElMotivo() {
                Organization active = organization(OrganizationStatus.ACTIVE);

                when(organizationRepository.findById(ORGANIZATION_ID))
                                .thenReturn(Optional.of(new OrganizationEntity(active)));
                when(organizationRepository.save(any(OrganizationEntity.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                autenticarComoAdmin();
                MDC.put(RequestCorrelationFilter.MDC_KEY, "org-status-123");

                Organization result = lifecycleService.changeStatus(
                                ORGANIZATION_ID,
                                OrganizationStatus.SUSPENDED,
                                SUSPENSION_REASON);

                assertEquals(OrganizationStatus.SUSPENDED, result.status());

                ArgumentCaptor<OrganizationStatusAuditEntity> captor = ArgumentCaptor
                                .forClass(OrganizationStatusAuditEntity.class);

                verify(auditRepository).save(captor.capture());

                OrganizationStatusAuditEntity event = captor.getValue();
                assertEquals(ORGANIZATION_ID, event.getOrganizationId());
                assertEquals(OrganizationStatusAuditAction.SUSPENDED, event.getAction());
                assertEquals("admin", event.getActor());
                assertEquals("org-status-123", event.getRequestId());
                assertEquals(SUSPENSION_REASON, event.getReason());
                assertNotNull(event.getOccurredAt());
        }

        @Test
        void deberiaReactivarUnaOrganizacionSuspendidaYRegistrarElMotivo() {
                Organization suspended = organization(OrganizationStatus.SUSPENDED);

                when(organizationRepository.findById(ORGANIZATION_ID))
                                .thenReturn(Optional.of(new OrganizationEntity(suspended)));
                when(organizationRepository.save(any(OrganizationEntity.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                autenticarComoAdmin();

                Organization result = lifecycleService.changeStatus(
                                ORGANIZATION_ID,
                                OrganizationStatus.ACTIVE,
                                REACTIVATION_REASON);

                assertEquals(OrganizationStatus.ACTIVE, result.status());

                ArgumentCaptor<OrganizationStatusAuditEntity> captor = ArgumentCaptor
                                .forClass(OrganizationStatusAuditEntity.class);

                verify(auditRepository).save(captor.capture());
                assertEquals(
                                OrganizationStatusAuditAction.REACTIVATED,
                                captor.getValue().getAction());
                assertEquals(
                                REACTIVATION_REASON,
                                captor.getValue().getReason());
        }

        @Test
        void noDeberiaGuardarSiElEstadoYaEsElSolicitado() {
                Organization active = organization(OrganizationStatus.ACTIVE);

                when(organizationRepository.findById(ORGANIZATION_ID))
                                .thenReturn(Optional.of(new OrganizationEntity(active)));

                Organization result = lifecycleService.changeStatus(
                                ORGANIZATION_ID,
                                OrganizationStatus.ACTIVE,
                                "Organización ya revisada");

                assertEquals(OrganizationStatus.ACTIVE, result.status());
                verify(organizationRepository, never()).save(any(OrganizationEntity.class));
                verify(auditRepository, never()).save(any(OrganizationStatusAuditEntity.class));
        }

        @Test
        void deberiaRechazarUnaOrganizacionInexistente() {
                when(organizationRepository.findById(ORGANIZATION_ID))
                                .thenReturn(Optional.empty());

                assertThrows(
                                OrganizationNotFoundException.class,
                                () -> lifecycleService.changeStatus(
                                                ORGANIZATION_ID,
                                                OrganizationStatus.SUSPENDED,
                                                "Incidente confirmado por revisión"));

                verify(organizationRepository, never()).save(any(OrganizationEntity.class));
                verify(auditRepository, never()).save(any(OrganizationStatusAuditEntity.class));
        }

        @Test
        void deberiaRechazarUnMotivoDemasiadoCortoAntesDeConsultarLaOrganizacion() {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> lifecycleService.changeStatus(
                                                ORGANIZATION_ID,
                                                OrganizationStatus.SUSPENDED,
                                                "corto"));

                verifyNoInteractions(organizationRepository, auditRepository);
        }

        private Organization organization(OrganizationStatus status) {
                return new Organization(
                                ORGANIZATION_ID,
                                "Secure Findings",
                                "secure-findings",
                                CREATED_AT,
                                status);
        }

        private void autenticarComoAdmin() {
                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(
                                                "admin",
                                                "n/a",
                                                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        }

        @Test
        void deberiaRechazarCincoEmojisComoMotivoDemasiadoCorto() {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> lifecycleService.changeStatus(
                                                ORGANIZATION_ID,
                                                OrganizationStatus.SUSPENDED,
                                                "😀".repeat(5)));

                verifyNoInteractions(organizationRepository, auditRepository);
        }
}