package com.securefindings.audit.domain;

import java.time.Instant;
import java.util.UUID;

public record AuditEvent(
        UUID findingId,
        AuditAction action,
        String actor,
        Instant occurredAt) {
}