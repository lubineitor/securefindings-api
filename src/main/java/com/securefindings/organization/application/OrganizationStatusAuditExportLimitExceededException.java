package com.securefindings.organization.application;

public class OrganizationStatusAuditExportLimitExceededException
        extends RuntimeException {

    private final int maximumRows;

    public OrganizationStatusAuditExportLimitExceededException(int maximumRows) {
        super("La exportación supera el máximo de "
                + maximumRows
                + " eventos de auditoría de estado de organizaciones");
        this.maximumRows = maximumRows;
    }

    public int maximumRows() {
        return maximumRows;
    }
}