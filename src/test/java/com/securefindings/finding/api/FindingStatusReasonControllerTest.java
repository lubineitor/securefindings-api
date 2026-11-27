package com.securefindings.finding.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.securefindings.finding.application.FindingService;
import com.securefindings.finding.domain.Finding;
import com.securefindings.finding.domain.FindingSeverity;
import com.securefindings.finding.domain.FindingStatus;

@ExtendWith(MockitoExtension.class)
class FindingStatusReasonControllerTest {

    private static final UUID FINDING_ID = UUID.fromString(
            "11111111-1111-1111-1111-111111111111");

    private static final String REASON = "Se confirmó la vulnerabilidad durante la prueba manual";

    @Mock
    private FindingService findingService;

    @Test
    void deberiaEnviarElMotivoAlServicioEnLaActualizacionIndividual() {
        Finding expected = finding(FINDING_ID);
        when(findingService.updateStatus(
                FINDING_ID,
                FindingStatus.IN_PROGRESS,
                REASON))
                .thenReturn(expected);

        FindingController controller = new FindingController(
                findingService,
                mock(FindingCsvExporter.class));

        Finding actual = controller.updateStatus(
                FINDING_ID,
                new UpdateFindingStatusRequest(
                        FindingStatus.IN_PROGRESS,
                        "  " + REASON + "  "));

        assertSame(expected, actual);
        verify(findingService).updateStatus(
                FINDING_ID,
                FindingStatus.IN_PROGRESS,
                REASON);
    }

    @Test
    void deberiaEnviarElMotivoAlServicioEnLaActualizacionEnLote() {
        List<UUID> ids = List.of(FINDING_ID);
        Finding updatedFinding = finding(FINDING_ID);
        when(findingService.updateStatuses(
                ids,
                FindingStatus.IN_PROGRESS,
                REASON))
                .thenReturn(List.of(updatedFinding));

        FindingBulkStatusController controller = new FindingBulkStatusController(findingService);

        BulkFindingStatusResponse response = controller.updateStatuses(
                new UpdateFindingStatusesRequest(
                        ids,
                        FindingStatus.IN_PROGRESS,
                        "  " + REASON + "  "));

        assertEquals(1, response.updatedCount());
        assertEquals(List.of(FINDING_ID), response.findingIds());
        verify(findingService).updateStatuses(
                ids,
                FindingStatus.IN_PROGRESS,
                REASON);
    }

    private Finding finding(UUID id) {
        Instant timestamp = Instant.parse("2026-11-27T10:00:00Z");
        return new Finding(
                id,
                "Hallazgo de prueba",
                "Descripción del hallazgo de prueba",
                FindingSeverity.HIGH,
                FindingStatus.IN_PROGRESS,
                timestamp,
                timestamp);
    }
}