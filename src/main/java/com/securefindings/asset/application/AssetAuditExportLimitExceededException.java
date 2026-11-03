package com.securefindings.asset.application;

public class AssetAuditExportLimitExceededException extends RuntimeException {

    private final int maximumRows;

    public AssetAuditExportLimitExceededException(int maximumRows) {
        super(
                "La exportación supera el máximo de "
                        + maximumRows
                        + " eventos de auditoría de activos");
        this.maximumRows = maximumRows;
    }

    public int maximumRows() {
        return maximumRows;
    }
}