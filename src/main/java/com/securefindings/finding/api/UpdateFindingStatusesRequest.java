package com.securefindings.finding.api;

import java.util.List;
import java.util.UUID;

import com.securefindings.finding.domain.FindingStatus;
import com.securefindings.organization.api.validation.CodePointSize;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateFindingStatusesRequest(
                @NotNull(message = "La lista de hallazgos es obligatoria") @Size(min = 1, max = 100, message = "La lista debe contener entre 1 y 100 hallazgos") List<@NotNull(message = "El identificador del hallazgo no puede ser nulo") UUID> findingIds,

                @NotNull(message = "El estado es obligatorio") FindingStatus status,

                @CodePointSize(min = 10, max = 500, message = "El motivo debe tener entre 10 y 500 caracteres") String reason) {

        public UpdateFindingStatusesRequest {
                reason = normalizeReason(reason);
        }

        public UpdateFindingStatusesRequest(
                        List<UUID> findingIds,
                        FindingStatus status) {
                this(findingIds, status, null);
        }

        private static String normalizeReason(String reason) {
                if (reason == null) {
                        return null;
                }

                String normalized = reason.strip();
                return normalized.isEmpty() ? null : normalized;
        }
}