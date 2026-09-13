package com.securefindings.finding.application;

import java.util.UUID;

import com.securefindings.finding.domain.FindingStatus;

public class FindingStatusTransitionException
        extends RuntimeException {

    public FindingStatusTransitionException(
            UUID findingId,
            FindingStatus currentStatus,
            FindingStatus targetStatus) {

        super(
                "No se puede cambiar el estado del hallazgo "
                        + findingId
                        + " de "
                        + currentStatus
                        + " a "
                        + targetStatus);
    }
}