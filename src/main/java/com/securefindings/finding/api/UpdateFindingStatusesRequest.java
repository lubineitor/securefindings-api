package com.securefindings.finding.api;

import java.util.List;
import java.util.UUID;

import com.securefindings.finding.domain.FindingStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateFindingStatusesRequest(
        @NotNull(message = "La lista de hallazgos es obligatoria") @Size(min = 1, max = 100, message = "La lista debe contener entre 1 y 100 hallazgos") List<@NotNull(message = "El identificador del hallazgo no puede ser nulo") UUID> findingIds,

        @NotNull(message = "El estado es obligatorio") FindingStatus status) {
}