package com.securefindings.finding.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.securefindings.finding.domain.Finding;
import com.securefindings.finding.domain.FindingSeverity;
import com.securefindings.finding.domain.FindingStatus;

class FindingCsvExporterTest {

    private final FindingCsvExporter exporter = new FindingCsvExporter();

    @Test
    void deberiaGenerarCsvConCabeceraYEscaparComasComillasYSaltosDeLinea() {
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Instant timestamp = Instant.parse("2026-10-19T10:15:30Z");
        Finding finding = new Finding(
                id,
                "Título, con coma",
                "Línea 1\nLínea \"2\"",
                FindingSeverity.HIGH,
                FindingStatus.OPEN,
                timestamp,
                timestamp);

        String csv = exporter.toCsv(List.of(finding));

        String expected = "\"id\",\"title\",\"description\",\"severity\",\"status\",\"createdAt\",\"updatedAt\"\r\n"
                + "\"11111111-1111-1111-1111-111111111111\",\"Título, con coma\",\"Línea 1\nLínea \"\"2\"\"\",\"HIGH\",\"OPEN\",\"2026-10-19T10:15:30Z\",\"2026-10-19T10:15:30Z\"\r\n";

        assertEquals(expected, csv);
    }

    @Test
    void deberiaNeutralizarCeldasQuePodrianEjecutarseComoFormulas() {
        Instant timestamp = Instant.parse("2026-10-19T10:15:30Z");
        Finding finding = new Finding(
                UUID.randomUUID(),
                "  =1+1",
                "@SUM(1,1)",
                FindingSeverity.MEDIUM,
                FindingStatus.OPEN,
                timestamp,
                timestamp);

        String csv = exporter.toCsv(List.of(finding));

        assertTrue(csv.contains("\"'  =1+1\""));
        assertTrue(csv.contains("\"'@SUM(1,1)\""));
    }

    @Test
    void deberiaNeutralizarFormulasPrecedidasPorTabuladores() {
        Instant timestamp = Instant.parse("2026-10-19T10:15:30Z");
        Finding finding = new Finding(
                UUID.randomUUID(),
                "\t=1+1",
                "Descripción normal",
                FindingSeverity.LOW,
                FindingStatus.OPEN,
                timestamp,
                timestamp);

        String csv = exporter.toCsv(List.of(finding));

        assertTrue(csv.contains("\"'\t=1+1\""));
    }
}