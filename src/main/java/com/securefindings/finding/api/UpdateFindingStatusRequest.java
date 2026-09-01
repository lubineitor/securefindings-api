package com.securefindings.finding.api;

import jakarta.validation.constraints.NotNull;

import com.securefindings.finding.domain.FindingStatus;

public record UpdateFindingStatusRequest(

                @NotNull(message = "El estado es obligatorio") FindingStatus status) {
}