package com.securefindings.organization.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.securefindings.organization.domain.OrganizationStatusAuditAction;
import com.securefindings.organization.domain.OrganizationStatusAuditEvent;
import com.securefindings.organization.persistence.OrganizationStatusAuditEntity;

class OrganizationStatusAuditCsvExporterTest {

        private static final UUID ORGANIZATION_ID = UUID.fromString(
                        "00000000-0000-0000-0000-000000000001");

        private final OrganizationStatusAuditCsvExporter exporter = new OrganizationStatusAuditCsvExporter();

        @Test
        void deberiaEscaparComasYComillas() {
                OrganizationStatusAuditEntity event = new OrganizationStatusAuditEntity(
                                new OrganizationStatusAuditEvent(
                                                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                                                ORGANIZATION_ID,
                                                OrganizationStatusAuditAction.SUSPENDED,
                                                "admin, \"externo\"",
                                                Instant.parse("2026-11-10T10:00:00Z"),
                                                "status-request-1",
                                                "Suspensión tras incidente confirmado"));

                String expected = "\"id\",\"organizationId\",\"action\",\"actor\",\"occurredAt\",\"requestId\",\"reason\"\r\n"
                                + "\"11111111-1111-1111-1111-111111111111\","
                                + "\"00000000-0000-0000-0000-000000000001\","
                                + "\"SUSPENDED\","
                                + "\"admin, \"\"externo\"\"\","
                                + "\"2026-11-10T10:00:00Z\","
                                + "\"status-request-1\","
                                + "\"Suspensión tras incidente confirmado\"\r\n";

                assertEquals(expected, exporter.toCsv(List.of(event)));
        }

        @Test
        void deberiaNeutralizarCeldasQuePuedenEjecutarFormulas() {
                OrganizationStatusAuditEntity event = new OrganizationStatusAuditEntity(
                                new OrganizationStatusAuditEvent(
                                                UUID.randomUUID(),
                                                ORGANIZATION_ID,
                                                OrganizationStatusAuditAction.SUSPENDED,
                                                "=1+1",
                                                Instant.parse("2026-11-10T10:00:00Z"),
                                                "status-request-2"));

                String csv = exporter.toCsv(List.of(event));

                assertTrue(csv.contains("\"'=1+1\""));
        }

        @Test
        void deberiaDejarRequestIdVacioCuandoEsNulo() {
                OrganizationStatusAuditEntity event = new OrganizationStatusAuditEntity(
                                new OrganizationStatusAuditEvent(
                                                UUID.randomUUID(),
                                                ORGANIZATION_ID,
                                                OrganizationStatusAuditAction.REACTIVATED,
                                                "admin",
                                                Instant.parse("2026-11-11T10:00:00Z"),
                                                null));

                String csv = exporter.toCsv(List.of(event));

                assertTrue(csv.endsWith("\"\"\r\n"));
        }

        @Test
        void deberiaDevolverLaCabeceraCuandoNoHayEventos() {
                assertEquals(
                                "\"id\",\"organizationId\",\"action\",\"actor\",\"occurredAt\",\"requestId\",\"reason\"\r\n",
                                exporter.toCsv(List.of()));
        }

        @Test
        void deberiaNeutralizarMotivosQuePuedenEjecutarFormulas() {
                OrganizationStatusAuditEntity event = new OrganizationStatusAuditEntity(
                                new OrganizationStatusAuditEvent(
                                                UUID.randomUUID(),
                                                ORGANIZATION_ID,
                                                OrganizationStatusAuditAction.SUSPENDED,
                                                "admin",
                                                Instant.parse("2026-11-10T10:00:00Z"),
                                                "status-request-2",
                                                "=SUM(1,1)+1"));

                String csv = exporter.toCsv(List.of(event));

                assertTrue(csv.contains("\"'=SUM(1,1)+1\""));
        }
}