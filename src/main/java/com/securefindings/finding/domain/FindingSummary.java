package com.securefindings.finding.domain;

import java.util.Map;
import java.util.Objects;

public record FindingSummary(
        long total,
        Map<FindingStatus, Long> byStatus,
        Map<FindingSeverity, Long> bySeverity) {

    public FindingSummary {
        byStatus = Map.copyOf(Objects.requireNonNull(byStatus));
        bySeverity = Map.copyOf(Objects.requireNonNull(bySeverity));
    }
}