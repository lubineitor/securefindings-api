package com.securefindings.finding.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record FindingOverdueSummary(
        Instant asOf,
        long total,
        Map<FindingStatus, Long> byStatus,
        Map<FindingSeverity, Long> bySeverity) {

    public FindingOverdueSummary {
        Objects.requireNonNull(asOf, "La fecha de cálculo no puede ser nula");
        byStatus = Map.copyOf(Objects.requireNonNull(byStatus));
        bySeverity = Map.copyOf(Objects.requireNonNull(bySeverity));
    }
}