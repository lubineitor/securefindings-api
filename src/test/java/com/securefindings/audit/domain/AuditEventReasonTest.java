package com.securefindings.audit.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.securefindings.finding.domain.FindingStatus;

class AuditEventReasonTest {

    private static final UUID FINDING_ID = UUID.fromString(
            "11111111-1111-1111-1111-111111111111");

    private static final UUID ORGANIZATION_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000001");

    private static final Instant OCCURRED_AT = Instant.parse("2026-11-27T10:00:00Z");

    @Test
    void deberiaRecortarElMotivoDeUnaTransicion() {
        AuditEvent event = statusChange("  Motivo detallado de la transición  ");

        assertEquals("Motivo detallado de la transición", event.reason());
    }

    @Test
    void deberiaPermitirUnaTransicionSinMotivo() {
        AuditEvent event = statusChange(null);

        assertNull(event.reason());
    }

    @Test
    void deberiaRechazarUnMotivoDemasiadoCorto() {
        assertThrows(
                IllegalArgumentException.class,
                () -> statusChange("Causa"));
    }

    @Test
    void deberiaRechazarUnMotivoEnUnaAccionQueNoEsTransicion() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AuditEvent(
                        FINDING_ID,
                        ORGANIZATION_ID,
                        AuditAction.CREATED,
                        "analista",
                        OCCURRED_AT,
                        null,
                        null,
                        null,
                        "Motivo que no corresponde a una transición"));
    }

    private AuditEvent statusChange(String reason) {
        return new AuditEvent(
                FINDING_ID,
                ORGANIZATION_ID,
                AuditAction.STATUS_CHANGED,
                "analista",
                OCCURRED_AT,
                null,
                FindingStatus.OPEN,
                FindingStatus.IN_PROGRESS,
                reason);
    }
}