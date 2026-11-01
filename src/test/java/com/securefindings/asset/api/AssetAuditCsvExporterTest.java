package com.securefindings.asset.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.securefindings.asset.domain.AssetAuditAction;
import com.securefindings.asset.domain.AssetAuditEvent;
import com.securefindings.asset.persistence.AssetAuditEntity;

class AssetAuditCsvExporterTest {

    private static final UUID ORGANIZATION_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000001");

    private final AssetAuditCsvExporter exporter = new AssetAuditCsvExporter();

    @Test
    void deberiaEscaparComasYComillasEnLosValores() {
        Instant occurredAt = Instant.parse("2026-10-19T10:15:30Z");

        AssetAuditEntity event = new AssetAuditEntity(
                new AssetAuditEvent(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        UUID.fromString("22222222-2222-2222-2222-222222222222"),
                        ORGANIZATION_ID,
                        AssetAuditAction.CREATED,
                        "usuario, \"externo\"",
                        occurredAt,
                        "asset-request-123"));

        String csv = exporter.toCsv(List.of(event));

        String expected = "\"id\",\"assetId\",\"action\",\"actor\",\"occurredAt\",\"requestId\"\r\n"
                + "\"11111111-1111-1111-1111-111111111111\","
                + "\"22222222-2222-2222-2222-222222222222\","
                + "\"CREATED\","
                + "\"usuario, \"\"externo\"\"\","
                + "\"2026-10-19T10:15:30Z\","
                + "\"asset-request-123\"\r\n";

        assertEquals(expected, csv);
    }

    @Test
    void deberiaNeutralizarValoresQuePodrianEjecutarseComoFormulas() {
        Instant occurredAt = Instant.parse("2026-10-19T10:15:30Z");

        AssetAuditEntity event = new AssetAuditEntity(
                new AssetAuditEvent(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        ORGANIZATION_ID,
                        AssetAuditAction.CREATED,
                        "=1+1",
                        occurredAt,
                        "@SUM(1,1)"));

        String csv = exporter.toCsv(List.of(event));

        assertTrue(csv.contains("\"'=1+1\""));
        assertTrue(csv.contains("\"'@SUM(1,1)\""));
    }

    @Test
    void deberiaDevolverLaCabeceraCuandoNoHayEventos() {
        assertEquals(
                "\"id\",\"assetId\",\"action\",\"actor\",\"occurredAt\",\"requestId\"\r\n",
                exporter.toCsv(List.of()));
    }
}