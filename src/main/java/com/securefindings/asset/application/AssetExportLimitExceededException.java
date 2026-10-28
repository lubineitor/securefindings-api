package com.securefindings.asset.application;

public class AssetExportLimitExceededException extends RuntimeException {

    private final int maximumRows;

    public AssetExportLimitExceededException(int maximumRows) {
        super("La exportación supera el máximo de " + maximumRows + " activos");
        this.maximumRows = maximumRows;
    }

    public int maximumRows() {
        return maximumRows;
    }
}