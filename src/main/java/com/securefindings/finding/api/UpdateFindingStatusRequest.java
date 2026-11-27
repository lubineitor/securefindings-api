package com.securefindings.finding.api;

import com.securefindings.finding.domain.FindingStatus;
import com.securefindings.organization.api.validation.CodePointSize;

import jakarta.validation.constraints.NotNull;

public record UpdateFindingStatusRequest(
        @NotNull(message = "El estado es obligatorio") FindingStatus status,
        @CodePointSize(min = 10, max = 500, message = "El motivo debe tener entre 10 y 500 caracteres") String reason) {

    public UpdateFindingStatusRequest {
        reason = normalizeReason(reason);
    }

    public UpdateFindingStatusRequest(FindingStatus status) {
        this(status, null);
    }

    private static String normalizeReason(String reason) {
        if (reason == null) {
            return null;
        }

        String normalized = reason.strip();
        return normalized.isEmpty() ? null : normalized;
    }
}