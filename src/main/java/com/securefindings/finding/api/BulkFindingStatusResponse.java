package com.securefindings.finding.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.securefindings.finding.domain.Finding;
import com.securefindings.finding.domain.FindingStatus;

public record BulkFindingStatusResponse(
        int updatedCount,
        FindingStatus status,
        List<UUID> findingIds) {

    public BulkFindingStatusResponse {
        findingIds = List.copyOf(findingIds);
    }

    public static BulkFindingStatusResponse from(
            FindingStatus status,
            List<Finding> findings) {

        Objects.requireNonNull(status, "El estado no puede ser nulo");
        Objects.requireNonNull(findings, "La lista de hallazgos no puede ser nula");

        List<UUID> ids = new ArrayList<>(findings.size());

        for (Finding finding : findings) {
            Finding checkedFinding = Objects.requireNonNull(
                    finding,
                    "La lista contiene un hallazgo nulo");

            ids.add(checkedFinding.id());
        }

        return new BulkFindingStatusResponse(
                ids.size(),
                status,
                ids);
    }
}