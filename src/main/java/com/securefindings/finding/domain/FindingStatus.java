package com.securefindings.finding.domain;

public enum FindingStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    FALSE_POSITIVE;

    public boolean canTransitionTo(FindingStatus targetStatus) {
        if (targetStatus == null) {
            return false;
        }

        if (this == targetStatus) {
            return true;
        }

        return switch (this) {
            case OPEN -> targetStatus == IN_PROGRESS
                    || targetStatus == RESOLVED
                    || targetStatus == FALSE_POSITIVE;

            case IN_PROGRESS -> targetStatus == OPEN
                    || targetStatus == RESOLVED
                    || targetStatus == FALSE_POSITIVE;

            case RESOLVED, FALSE_POSITIVE -> targetStatus == OPEN;
        };
    }
}