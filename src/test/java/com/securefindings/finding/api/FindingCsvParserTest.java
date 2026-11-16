package com.securefindings.finding.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class FindingCsvParserTest {

    private final FindingCsvParser parser = new FindingCsvParser();

    @Test
    void deberiaLeerComillasComasYSaltosDeLinea() {
        UUID assetId = UUID.fromString(
                "22222222-2222-2222-2222-222222222222");

        String csv = """
                title,description,severity,assetId,dueAt
                "SQL, Injection","Texto con ""comillas""
                y una segunda línea",HIGH,%s,2026-12-01T09:00:00Z
                """.formatted(assetId);

        FindingCsvParser.ParseResult result = parser.parse(
                csv.getBytes(StandardCharsets.UTF_8));

        assertTrue(result.errors().isEmpty());
        assertEquals(1, result.rows().size());

        FindingCsvParser.ParsedRow row = result.rows().get(0);

        assertEquals("SQL, Injection", row.title());
        assertEquals(
                "Texto con \"comillas\"\ny una segunda línea",
                row.description());
        assertEquals(assetId, row.assetId());
        assertEquals(
                Instant.parse("2026-12-01T09:00:00Z"),
                row.dueAt());
    }

    @Test
    void deberiaDevolverErroresParaLosCamposInvalidos() {
        String csv = """
                title,description,severity,assetId,dueAt
                ,Descripción,UNKNOWN,no-es-uuid,no-es-fecha
                """;

        FindingCsvParser.ParseResult result = parser.parse(
                csv.getBytes(StandardCharsets.UTF_8));

        assertTrue(result.rows().isEmpty());
        assertEquals(4, result.errors().size());
        assertTrue(result.errors().stream()
                .anyMatch(error -> error.field().equals("title")));
        assertTrue(result.errors().stream()
                .anyMatch(error -> error.field().equals("severity")));
        assertTrue(result.errors().stream()
                .anyMatch(error -> error.field().equals("assetId")));
        assertTrue(result.errors().stream()
                .anyMatch(error -> error.field().equals("dueAt")));
        assertTrue(result.errors().stream()
                .allMatch(error -> error.rowNumber() == 2));
    }

    @Test
    void deberiaRechazarUnFicheroConCabeceraIncorrecta() {
        String csv = """
                id,title,description
                """;

        FindingCsvParser.ParseResult result = parser.parse(
                csv.getBytes(StandardCharsets.UTF_8));

        assertTrue(result.rows().isEmpty());
        assertEquals(1, result.errors().size());
        assertEquals("header", result.errors().get(0).field());
    }

    @Test
    void deberiaRechazarUnCampoEntreComillasSinCerrar() {
        String csv = """
                title,description,severity,assetId,dueAt
                "Título,"Descripción",HIGH,,
                """;

        FindingCsvParser.ParseResult result = parser.parse(
                csv.getBytes(StandardCharsets.UTF_8));

        assertTrue(result.rows().isEmpty());
        assertEquals(1, result.errors().size());
        assertEquals("file", result.errors().get(0).field());
    }

    @Test
    void deberiaRechazarMasDeMilFilas() {
        StringBuilder csv = new StringBuilder(
                "title,description,severity,assetId,dueAt\r\n");

        for (int index = 0; index <= FindingCsvParser.MAX_ROWS; index++) {
            csv.append("Hallazgo ")
                    .append(index)
                    .append(",Descripción,HIGH,,\r\n");
        }

        FindingCsvParser.ParseResult result = parser.parse(
                csv.toString().getBytes(StandardCharsets.UTF_8));

        assertFalse(result.errors().isEmpty());
        assertTrue(result.errors().stream()
                .anyMatch(error -> error.field().equals("file")));
    }

    @Test
    void deberiaRechazarUnFicheroMayorDeCincoMib() {
        byte[] csv = new byte[FindingCsvParser.MAX_FILE_SIZE_BYTES + 1];

        FindingCsvParser.ParseResult result = parser.parse(csv);

        assertTrue(result.rows().isEmpty());
        assertEquals(1, result.errors().size());
        assertEquals("file", result.errors().get(0).field());
    }

    @Test
    void deberiaRechazarUtf8Malformado() {
        byte[] csv = new byte[] {
                (byte) 0xC3,
                (byte) 0x28
        };

        FindingCsvParser.ParseResult result = parser.parse(csv);

        assertTrue(result.rows().isEmpty());
        assertEquals(1, result.errors().size());
        assertEquals("file", result.errors().get(0).field());
    }
}