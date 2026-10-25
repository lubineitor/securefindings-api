package com.securefindings.asset.application;

import java.util.UUID;

public class AssetArchivedException extends RuntimeException {

    private final UUID assetId;

    public AssetArchivedException(UUID assetId) {
        super("No se pueden asociar nuevos hallazgos a un activo archivado");
        this.assetId = assetId;
    }

    public UUID assetId() {
        return assetId;
    }
}