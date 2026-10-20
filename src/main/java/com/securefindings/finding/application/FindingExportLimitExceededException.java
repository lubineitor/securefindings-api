package com.securefindings.finding.application;

public class FindingExportLimitExceededException extends RuntimeException {

    private final int maximumRows;

    public FindingExportLimitExceededException(int maximumRows) {
        super("La exportación supera el máximo de " + maximumRows + " hallazgos");
        this.maximumRows = maximumRows;
    }

    public int maximumRows() {
        return maximumRows;
    }
}