package com.securefindings.finding.api;

import com.securefindings.finding.domain.FindingSeverity;

public record CreateFindingRequest(
        String title,
        String description,
        FindingSeverity severity
) {
}