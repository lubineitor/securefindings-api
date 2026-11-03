package com.securefindings.asset.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.securefindings.asset.domain.Asset;
import com.securefindings.asset.domain.AssetCriticality;
import com.securefindings.asset.domain.AssetEnvironment;
import com.securefindings.asset.domain.AssetStatus;
import com.securefindings.asset.domain.AssetType;

class AssetCsvExporterTest {

    private final AssetCsvExporter exporter = new AssetCsvExporter();

    @Test
    void deberiaGenerarCsvConCabeceraYEscalarComasYComillas() {
        UUID id = UUID.fromString(
                "11111111-1111-1111-1111-111111111111");
        Instant timestamp = Instant.parse("2026-10-19T10:15:30Z");

        Asset asset = new Asset(
                id,
                "API, \"pagos\"",
                "https://payments.example.test",
                AssetType.API,
                AssetEnvironment.PRODUCTION,
                AssetCriticality.HIGH,
                AssetStatus.ACTIVE,
                timestamp,
                timestamp);

        String csv = exporter.toCsv(List.of(asset));

        String expected = "\"id\",\"name\",\"identifier\",\"type\",\"environment\",\"criticality\",\"status\",\"createdAt\",\"updatedAt\"\r\n"
                + "\"11111111-1111-1111-1111-111111111111\",\"API, \"\"pagos\"\"\",\"https://payments.example.test\",\"API\",\"PRODUCTION\",\"HIGH\",\"ACTIVE\",\"2026-10-19T10:15:30Z\",\"2026-10-19T10:15:30Z\"\r\n";

        assertEquals(expected, csv);
    }

    @Test
    void deberiaNeutralizarCeldasQuePodrianEjecutarseComoFormulas() {
        Instant timestamp = Instant.parse("2026-10-19T10:15:30Z");

        Asset asset = new Asset(
                UUID.randomUUID(),
                "=1+1",
                "@SUM(1,1)",
                AssetType.API,
                AssetEnvironment.PRODUCTION,
                AssetCriticality.HIGH,
                AssetStatus.ACTIVE,
                timestamp,
                timestamp);

        String csv = exporter.toCsv(List.of(asset));

        assertTrue(csv.contains("\"'=1+1\""));
        assertTrue(csv.contains("\"'@SUM(1,1)\""));
    }

    @Test
    void deberiaDevolverLaCabeceraSiNoHayActivos() {
        assertEquals(
                "\"id\",\"name\",\"identifier\",\"type\",\"environment\",\"criticality\",\"status\",\"createdAt\",\"updatedAt\"\r\n",
                exporter.toCsv(List.of()));
    }
}