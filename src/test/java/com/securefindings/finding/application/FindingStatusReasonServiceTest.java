package com.securefindings.finding.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
class FindingStatusReasonServiceTest {

    private static final UUID ORGANIZATION_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000001");

    private static final UUID FINDING_ID = UUID.fromString(
            "11111111-1111-1111-1111-111111111111");

    private static final UUID SECOND_FINDING_ID = UUID.fromString(
            "22222222-2222-2222-2222-222222222222");

    private static final String REASON = "Se confirmó la vulnerabilidad durante la prueba manual";

    @Mock
    private FindingRepository findingRepository;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private OrganizationContext organizationContext;

    @BeforeEach
    void configurarContexto() {
        SecurityContextHolder.clearContext();
        when(organizationContext.currentOrganizationId())
                .thenReturn(ORGANIZATION_ID);
        when(findingRepository.save(any(FindingEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deberiaRegistrarElMotivoEnUnaTransicionIndividual() {
        Finding current = finding(FINDING_ID, FindingStatus.OPEN);

        when(findingRepository.findByIdAndOrganizationId(
                FINDING_ID,
                ORGANIZATION_ID))
                .thenReturn(Optional.of(new FindingEntity(current, ORGANIZATION_ID)));

        FindingService service = service();

        Finding updated = service.updateStatus(
                FINDING_ID,
                FindingStatus.IN_PROGRESS,
                REASON);

        assertEquals(FindingStatus.IN_PROGRESS, updated.status());
        verify(auditService).registerStatusChange(
                FINDING_ID,
                FindingStatus.OPEN,
                FindingStatus.IN_PROGRESS,
                "system",
                REASON);
    }

    @Test
    void deberiaAplicarElMotivoATodasLasTransicionesDelLote() {
        List<UUID> ids = List.of(FINDING_ID, SECOND_FINDING_ID);
        Finding first = finding(FINDING_ID, FindingStatus.OPEN);
        Finding second = finding(SECOND_FINDING_ID, FindingStatus.OPEN);

        when(findingRepository.findAllByIdsAndOrganizationId(
                ids,
                ORGANIZATION_ID))
                .thenReturn(List.of(
                        new FindingEntity(first, ORGANIZATION_ID),
                        new FindingEntity(second, ORGANIZATION_ID)));

        FindingService service = service();

        List<Finding> updated = service.updateStatuses(
                ids,
                FindingStatus.IN_PROGRESS,
                REASON);

        assertEquals(2, updated.size());

        verify(auditService).registerStatusChange(
                FINDING_ID,
                FindingStatus.OPEN,
                FindingStatus.IN_PROGRESS,
                "system",
                REASON);
        verify(auditService).registerStatusChange(
                SECOND_FINDING_ID,
                FindingStatus.OPEN,
                FindingStatus.IN_PROGRESS,
                "system",
                REASON);
    }

    private FindingService service() {
        return new FindingService(
                findingRepository,
                assetRepository,
                auditService,
                organizationContext);
    }

    private Finding finding(UUID id, FindingStatus status) {
        Instant timestamp = Instant.parse("2026-11-27T10:00:00Z");
        return new Finding(
                id,
                "Hallazgo de prueba",
                "Descripción del hallazgo de prueba",
                FindingSeverity.HIGH,
                status,
                timestamp,
                timestamp);
    }
}