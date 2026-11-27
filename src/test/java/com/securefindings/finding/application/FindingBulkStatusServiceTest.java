package com.securefindings.finding.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import com.securefindings.asset.persistence.AssetRepository;
import com.securefindings.audit.application.AuditService;
import com.securefindings.finding.domain.Finding;
import com.securefindings.finding.domain.FindingSeverity;
import com.securefindings.finding.domain.FindingStatus;
import com.securefindings.finding.persistence.FindingEntity;
import com.securefindings.finding.persistence.FindingRepository;
import com.securefindings.security.OrganizationContext;

@ExtendWith(MockitoExtension.class)
class FindingBulkStatusServiceTest {

        private static final UUID ORGANIZATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

        private static final UUID FIRST_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

        private static final UUID SECOND_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

        private static final Instant CREATED_AT = Instant.parse("2026-11-01T10:00:00Z");

        @Mock
        private FindingRepository findingRepository;

        @Mock
        private AssetRepository assetRepository;

        @Mock
        private AuditService auditService;

        @Mock
        private OrganizationContext organizationContext;

        private FindingService findingService;

        @BeforeEach
        void crearServicio() {
                findingService = new FindingService(
                                findingRepository,
                                assetRepository,
                                auditService,
                                organizationContext);
        }

        @AfterEach
        void limpiarContexto() {
                SecurityContextHolder.clearContext();
        }

        @Test
        void deberiaActualizarYAuditarTodosLosHallazgosEnElOrdenSolicitado() {
                List<UUID> ids = List.of(FIRST_ID, SECOND_ID);

                when(organizationContext.currentOrganizationId())
                                .thenReturn(ORGANIZATION_ID);

                when(findingRepository.findAllByIdsAndOrganizationId(
                                ids,
                                ORGANIZATION_ID))
                                .thenReturn(List.of(
                                                findingEntity(SECOND_ID, FindingStatus.IN_PROGRESS),
                                                findingEntity(FIRST_ID, FindingStatus.OPEN)));

                when(findingRepository.save(any(FindingEntity.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                List<Finding> result = findingService.updateStatuses(
                                ids,
                                FindingStatus.RESOLVED);

                List<UUID> resultIds = new ArrayList<>(result.size());
                List<FindingStatus> resultStatuses = new ArrayList<>(result.size());

                for (Finding finding : result) {
                        Finding checkedFinding = Objects.requireNonNull(
                                        finding,
                                        "El resultado contiene un hallazgo nulo");

                        resultIds.add(checkedFinding.id());
                        resultStatuses.add(checkedFinding.status());
                }

                assertEquals(ids, resultIds);
                assertEquals(
                                List.of(FindingStatus.RESOLVED, FindingStatus.RESOLVED),
                                resultStatuses);

                verify(findingRepository, org.mockito.Mockito.times(2))
                                .save(any(FindingEntity.class));
                verify(auditService).registerStatusChange(
                                eq(FIRST_ID),
                                eq(FindingStatus.OPEN),
                                eq(FindingStatus.RESOLVED),
                                anyString());

                verify(auditService).registerStatusChange(
                                eq(SECOND_ID),
                                eq(FindingStatus.IN_PROGRESS),
                                eq(FindingStatus.RESOLVED),
                                anyString());
        }

        @Test
        void noDeberiaConsultarElRepositorioSiHayIdsDuplicados() {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> findingService.updateStatuses(
                                                List.of(FIRST_ID, FIRST_ID),
                                                FindingStatus.IN_PROGRESS));

                verifyNoInteractions(
                                organizationContext,
                                findingRepository,
                                auditService);
        }

        @Test
        void noDeberiaActualizarSiFaltaUnHallazgoDeLaOrganizacion() {
                List<UUID> ids = List.of(FIRST_ID, SECOND_ID);

                when(organizationContext.currentOrganizationId())
                                .thenReturn(ORGANIZATION_ID);

                when(findingRepository.findAllByIdsAndOrganizationId(
                                ids,
                                ORGANIZATION_ID))
                                .thenReturn(List.of(
                                                findingEntity(FIRST_ID, FindingStatus.OPEN)));

                assertThrows(
                                FindingNotFoundException.class,
                                () -> findingService.updateStatuses(
                                                ids,
                                                FindingStatus.IN_PROGRESS));

                verify(findingRepository, never()).save(any(FindingEntity.class));
                verifyNoInteractions(auditService);
        }

        @Test
        void noDeberiaActualizarNingunHallazgoSiUnaTransicionNoEsValida() {
                List<UUID> ids = List.of(FIRST_ID, SECOND_ID);

                when(organizationContext.currentOrganizationId())
                                .thenReturn(ORGANIZATION_ID);

                when(findingRepository.findAllByIdsAndOrganizationId(
                                ids,
                                ORGANIZATION_ID))
                                .thenReturn(List.of(
                                                findingEntity(FIRST_ID, FindingStatus.OPEN),
                                                findingEntity(SECOND_ID, FindingStatus.RESOLVED)));

                assertThrows(
                                FindingStatusTransitionException.class,
                                () -> findingService.updateStatuses(
                                                ids,
                                                FindingStatus.FALSE_POSITIVE));

                verify(findingRepository, never()).save(any(FindingEntity.class));
                verifyNoInteractions(auditService);
        }

        private FindingEntity findingEntity(UUID id, FindingStatus status) {
                Finding finding = new Finding(
                                id,
                                "Hallazgo " + id,
                                "Descripción del hallazgo",
                                FindingSeverity.HIGH,
                                status,
                                CREATED_AT,
                                CREATED_AT,
                                null,
                                null);

                return new FindingEntity(finding, ORGANIZATION_ID);
        }
}