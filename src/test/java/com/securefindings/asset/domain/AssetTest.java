package com.securefindings.asset.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class AssetTest {

    @Test
    void deberiaCrearElActivoNormalizandoLosEspacios() {
        Asset asset = Asset.create(
                "  API pagos  ",
                "  https://payments.example.test  ",
                AssetType.API,
                AssetEnvironment.PRODUCTION,
                AssetCriticality.HIGH);

        assertEquals("API pagos", asset.name());
        assertEquals("https://payments.example.test", asset.identifier());
        assertEquals(AssetStatus.ACTIVE, asset.status());
    }

    @Test
    void deberiaRechazarUnIdentificadorVacio() {
        Instant now = Instant.now();

        assertThrows(
                IllegalArgumentException.class,
                () -> new Asset(
                        UUID.randomUUID(),
                        "API pagos",
                        "   ",
                        AssetType.API,
                        AssetEnvironment.PRODUCTION,
                        AssetCriticality.HIGH,
                        AssetStatus.ACTIVE,
                        now,
                        now));
    }

    @Test
    void actualizarDeberiaConservarElIdentificadorYLaFechaDeCreacion() {
        Asset asset = Asset.create(
                "API pagos",
                "payments-api",
                AssetType.API,
                AssetEnvironment.STAGING,
                AssetCriticality.MEDIUM);

        Asset updated = asset.withDetails(
                "API pagos v2",
                "payments-api-v2",
                AssetType.API,
                AssetEnvironment.PRODUCTION,
                AssetCriticality.CRITICAL,
                AssetStatus.ARCHIVED);

        assertEquals(asset.id(), updated.id());
        assertEquals(asset.createdAt(), updated.createdAt());
        assertEquals("API pagos v2", updated.name());
        assertEquals(AssetStatus.ARCHIVED, updated.status());
    }
}