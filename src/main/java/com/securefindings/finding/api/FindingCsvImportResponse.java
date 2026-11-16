package com.securefindings.finding.api;

import java.util.List;
import java.util.UUID;

public record FindingCsvImportResponse(
        int importedCount,
        int validatedCount,
        boolean dryRun,
        List<UUID> findingIds,
        List<RowError> errors) {

    public FindingCsvImportResponse {
        findingIds = List.copyOf(findingIds);
        errors = List.copyOf(errors);
    }

    public record RowError(
            int rowNumber,
            String field,
            String message) {
    }
}