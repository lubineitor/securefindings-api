package com.securefindings.audit.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.securefindings.audit.domain.AuditAction;
import com.securefindings.audit.persistence.FindingAuditEntity;

class FindingAuditCsvExporterTest {

    private static final UUID ORGANIZATION_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000001");

    private final FindingAuditCsvExporter exporter = new FindingAuditCsvExporter();

    @Test
    void deberiaEscaparComasYComillasEnLosValores() {
        FindingAuditEntity event = new FindingAuditEntity(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                ORGANIZATION_ID,
                AuditAction.UPDATED,
                "usuario, \"externo\"",
                Instant.parse("2026-10-10T08:00:00Z"),
                "audit-request-123");

        String csv = exporter.toCsv(List.of(event));

        String expected = "\"id\",\"findingId\",\"action\",\"actor\",\"occurredAt\",\"requestId\"\r\n"
                + "\"11111111-1111-1111-1111-111111111111\","
                + "\"22222222-2222-2222-2222-222222222222\","
                + "\"UPDATED\","
                + "\"usuario, \"\"externo\"\"\","
                + "\"2026-10-10T08:00:00Z\","
                + "\"audit-request-123\"\r\n";

        assertEquals(expected, csv);
    }

    @Test
    void deberiaNeutralizarValoresQuePodrianEjecutarseComoFormulas() {
        FindingAuditEntity event = new FindingAuditEntity(
                UUID.randomUUID(),
                UUID.randomUUID(),
                ORGANIZATION_ID,
                AuditAction.UPDATED,
                "=1+1",
                Instant.parse("2026-10-10T08:00:00Z"),
                "audit-request-123");

        String csv = exporter.toCsv(List.of(event));

        assertTrue(csv.contains("\"'=1+1\""));
    }

    @Test
    void deberiaDejarRequestIdVacioCuandoEsNulo() {
        FindingAuditEntity event = new FindingAuditEntity(
                UUID.randomUUID(),
                UUID.randomUUID(),
                ORGANIZATION_ID,
                AuditAction.CREATED,
                "analista",
                Instant.parse("2026-10-10T08:00:00Z"),
                null);

        String csv = exporter.toCsv(List.of(event));

        assertTrue(csv.endsWith("\"\"\r\n"));
    }

    @Test
    void deberiaDevolverLaCabeceraCuandoNoHayEventos() {
        assertEquals(
                "\"id\",\"findingId\",\"action\",\"actor\",\"occurredAt\",\"requestId\"\r\n",
                exporter.toCsv(List.of()));
    }
}