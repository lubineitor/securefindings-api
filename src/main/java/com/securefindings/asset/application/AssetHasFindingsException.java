package com.securefindings.asset.application;

import java.util.UUID;

public class AssetHasFindingsException extends RuntimeException {

    private final UUID assetId;

    public AssetHasFindingsException(UUID assetId) {
        super("No se puede eliminar un activo asociado a hallazgos");
        this.assetId = assetId;
    }

    public UUID assetId() {
        return assetId;
    }
}