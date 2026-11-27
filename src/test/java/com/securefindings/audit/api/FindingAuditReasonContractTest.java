package com.securefindings.audit.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.securefindings.audit.domain.AuditAction;
import com.securefindings.audit.persistence.FindingAuditEntity;
import com.securefindings.finding.domain.FindingStatus;

class FindingAuditReasonContractTest {

    private static final UUID EVENT_ID = UUID.fromString(
            "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private static final UUID FINDING_ID = UUID.fromString(
            "11111111-1111-1111-1111-111111111111");

    private static final UUID ORGANIZATION_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000001");

    @Test
    void deberiaExponerElMotivoEnLaRespuestaYEnElCsv() {
        String reason = "Validado por \"pentest\", reproducido";
        FindingAuditEntity entity = new FindingAuditEntity(
                EVENT_ID,
                FINDING_ID,
                ORGANIZATION_ID,
                AuditAction.STATUS_CHANGED,
                "analista",
                Instant.parse("2026-11-27T10:00:00Z"),
                "status-req-1",
                FindingStatus.OPEN,
                FindingStatus.IN_PROGRESS,
                reason);

        FindingAuditResponse response = FindingAuditResponse.from(entity);
        String csv = new FindingAuditCsvExporter().toCsv(List.of(entity));

        assertEquals(reason, response.reason());
        assertTrue(csv.contains("\"newStatus\",\"reason\""));
        assertTrue(csv.contains("\"IN_PROGRESS\",\"Validado por \"\"pentest\"\", reproducido\""));
    }
}