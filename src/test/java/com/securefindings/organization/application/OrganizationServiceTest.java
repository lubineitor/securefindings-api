package com.securefindings.organization.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

        @Test
        void deberiaListarOrganizacionesOrdenadasYConPaginacion() {
                Organization first = new Organization(
                                ORGANIZATION_ID,
                                "Alpha Security",
                                "alpha-security",
                                Instant.parse("2026-10-01T10:00:00Z"));
                Organization second = new Organization(
                                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                                "Secure Findings",
                                "secure-findings",
                                Instant.parse("2026-10-01T10:00:00Z"));

                when(organizationRepository.findAll(any(Pageable.class)))
                                .thenAnswer(invocation -> {
                                        Pageable pageable = invocation.getArgument(0);
                                        return new PageImpl<>(
                                                        List.of(
                                                                        new OrganizationEntity(first),
                                                                        new OrganizationEntity(second)),
                                                        pageable,
                                                        2);
                                });

                Page<Organization> actual = organizationService.findOrganizations(0, 20);

                assertEquals(List.of(first, second), actual.getContent());
                assertEquals(2, actual.getTotalElements());

                ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
                verify(organizationRepository).findAll(pageableCaptor.capture());

                assertEquals(0, pageableCaptor.getValue().getPageNumber());
                assertEquals(20, pageableCaptor.getValue().getPageSize());
                assertEquals(
                                Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id")),
                                pageableCaptor.getValue().getSort());
        }

        @Test
        void deberiaConsultarUnaOrganizacionPorIdentificador() {
                Organization expected = new Organization(
                                ORGANIZATION_ID,
                                "Secure Findings",
                                "secure-findings",
                                Instant.parse("2026-10-01T10:00:00Z"));

                when(organizationRepository.findById(ORGANIZATION_ID))
                                .thenReturn(Optional.of(new OrganizationEntity(expected)));

                Organization actual = organizationService.getOrganization(ORGANIZATION_ID);

                assertEquals(expected, actual);
                verify(organizationRepository).findById(ORGANIZATION_ID);
        }

        @Test
        void deberiaLanzarExcepcionSiLaOrganizacionNoExiste() {
                when(organizationRepository.findById(ORGANIZATION_ID))
                                .thenReturn(Optional.empty());

                assertThrows(
                                OrganizationNotFoundException.class,
                                () -> organizationService.getOrganization(ORGANIZATION_ID));
        }

        @Test
        void deberiaActualizarElNombreSinCambiarElSlugNiLaFechaDeCreacion() {
                Instant createdAt = Instant.parse("2026-10-01T10:00:00Z");
                Organization original = new Organization(
                                ORGANIZATION_ID,
                                "Secure Findings",
                                "secure-findings",
                                createdAt);

                when(organizationRepository.findById(ORGANIZATION_ID))
                                .thenReturn(Optional.of(new OrganizationEntity(original)));
                when(organizationRepository.save(any(OrganizationEntity.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                Organization actual = organizationService.updateOrganization(
                                ORGANIZATION_ID,
                                "  Secure Findings Europe  ");

                assertEquals(
                                new Organization(
                                                ORGANIZATION_ID,
                                                "Secure Findings Europe",
                                                "secure-findings",
                                                createdAt),
                                actual);
                verify(organizationRepository).save(any(OrganizationEntity.class));
        }

        @Test
        void deberiaRechazarLaActualizacionSiLaOrganizacionNoExiste() {
                when(organizationRepository.findById(ORGANIZATION_ID))
                                .thenReturn(Optional.empty());

                assertThrows(
                                OrganizationNotFoundException.class,
                                () -> organizationService.updateOrganization(
                                                ORGANIZATION_ID,
                                                "Secure Findings Europe"));

                verify(organizationRepository, never())
                                .save(any(OrganizationEntity.class));
        }

        @Test
        void deberiaCrearOrganizacionConNombreNormalizado() {
                when(organizationRepository.existsBySlug("acme-security"))
                                .thenReturn(false);
                when(organizationRepository.saveAndFlush(any(OrganizationEntity.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                Organization actual = organizationService.createOrganization(
                                "  Acme Security  ",
                                "acme-security");

                assertNotNull(actual.id());
                assertEquals("Acme Security", actual.name());
                assertEquals("acme-security", actual.slug());
                assertNotNull(actual.createdAt());

                verify(organizationRepository).existsBySlug("acme-security");
                verify(organizationRepository).saveAndFlush(any(OrganizationEntity.class));
        }

        @Test
        void deberiaRechazarLaCreacionSiElSlugYaExiste() {
                when(organizationRepository.existsBySlug("acme-security"))
                                .thenReturn(true);

                OrganizationSlugAlreadyExistsException exception = assertThrows(
                                OrganizationSlugAlreadyExistsException.class,
                                () -> organizationService.createOrganization(
                                                "Acme Security",
                                                "acme-security"));

                assertEquals("acme-security", exception.slug());
                verify(organizationRepository, never())
                                .saveAndFlush(any(OrganizationEntity.class));
        }

        @Test
        void deberiaConvertirUnaColisionDeSlugEnConflictoDeDominio() {
                when(organizationRepository.existsBySlug("acme-security"))
                                .thenReturn(false);
                when(organizationRepository.saveAndFlush(any(OrganizationEntity.class)))
                                .thenThrow(new DataIntegrityViolationException("unique slug"));

                OrganizationSlugAlreadyExistsException exception = assertThrows(
                                OrganizationSlugAlreadyExistsException.class,
                                () -> organizationService.createOrganization(
                                                "Acme Security",
                                                "acme-security"));

                assertEquals("acme-security", exception.slug());
        }
}
