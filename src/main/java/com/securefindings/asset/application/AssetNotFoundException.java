package com.securefindings.asset.application;

import java.util.UUID;

public class AssetNotFoundException extends RuntimeException {

    private final UUID assetId;

    public AssetNotFoundException(UUID assetId) {
        super("No se encontró el activo solicitado");
        this.assetId = assetId;
    }

    public UUID assetId() {
        return assetId;
    }
}