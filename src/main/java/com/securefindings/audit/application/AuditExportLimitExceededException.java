package com.securefindings.audit.application;

public class AuditExportLimitExceededException extends RuntimeException {

    private final int maximumRows;

    public AuditExportLimitExceededException(int maximumRows) {
        super("La exportación supera el máximo de "
                + maximumRows
                + " eventos de auditoría");
        this.maximumRows = maximumRows;
    }

    public int maximumRows() {
        return maximumRows;
    }
}