package com.securefindings.organization.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.securefindings.api.error.GlobalExceptionHandler;
import com.securefindings.organization.application.OrganizationStatusAuditExportLimitExceededException;
import com.securefindings.organization.application.OrganizationStatusAuditQueryService;
import com.securefindings.organization.domain.OrganizationStatusAuditAction;
import com.securefindings.organization.domain.OrganizationStatusAuditEvent;
import com.securefindings.organization.persistence.OrganizationStatusAuditEntity;
import com.securefindings.security.SecurityConfig;

@WebMvcTest(controllers = OrganizationStatusAuditExportController.class)
@Import({
                SecurityConfig.class,
                GlobalExceptionHandler.class
})
@WithMockUser(username = "analista", roles = "ANALYST")
class OrganizationStatusAuditExportControllerTest {

        private static final UUID ORGANIZATION_ID = UUID.fromString(
                        "00000000-0000-0000-0000-000000000001");

        private static final UUID EVENT_ID = UUID.fromString(
                        "00000000-0000-0000-0000-000000000401");

        private static final Instant OCCURRED_FROM = Instant.parse("2026-11-01T00:00:00Z");

        private static final Instant OCCURRED_TO = Instant.parse("2026-11-30T23:59:59Z");

        @Autowired
        private WebApplicationContext context;

        @MockitoBean
        private OrganizationStatusAuditQueryService queryService;

        @MockitoBean
        private OrganizationStatusAuditCsvExporter csvExporter;

        private MockMvc mockMvc;

        @BeforeEach
        void configurarMockMvc() {
                mockMvc = MockMvcBuilders
                                .webAppContextSetup(context)
                                .apply(springSecurity())
                                .build();
        }

        @Test
        @WithMockUser(username = "administrador", roles = "ADMIN")
        void deberiaExportarCsvAplicandoFiltros() throws Exception {
                OrganizationStatusAuditEntity event = new OrganizationStatusAuditEntity(
                                new OrganizationStatusAuditEvent(
                                                EVENT_ID,
                                                ORGANIZATION_ID,
                                                OrganizationStatusAuditAction.SUSPENDED,
                                                "administrador",
                                                Instant.parse("2026-11-10T10:00:00Z"),
                                                "status-request-1"));

                List<OrganizationStatusAuditEntity> events = List.of(event);
                String csv = "\"id\",\"organizationId\",\"action\",\"actor\",\"occurredAt\",\"requestId\",\"reason\"\r\n"
                                + "\"00000000-0000-0000-0000-000000000401\","
                                + "\"00000000-0000-0000-0000-000000000001\","
                                + "\"SUSPENDED\",\"administrador\","
                                + "\"2026-11-10T10:00:00Z\",\"status-request-1\",\"\"\r\n";

                when(queryService.findForExport(
                                ORGANIZATION_ID,
                                OrganizationStatusAuditAction.SUSPENDED,
                                "status-request-1",
                                "administrador",
                                OCCURRED_FROM,
                                OCCURRED_TO))
                                .thenReturn(events);

                when(csvExporter.toCsv(events)).thenReturn(csv);

                mockMvc.perform(get(
                                "/api/v1/organizations/{organizationId}/status-audit/export",
                                ORGANIZATION_ID)
                                .param("action", "SUSPENDED")
                                .param("requestId", "status-request-1")
                                .param("actor", "administrador")
                                .param("occurredFrom", "2026-11-01T00:00:00Z")
                                .param("occurredTo", "2026-11-30T23:59:59Z"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentTypeCompatibleWith(
                                                MediaType.parseMediaType("text/csv")))
                                .andExpect(header().string(
                                                "Content-Disposition",
                                                "attachment; filename=\"securefindings-organization-status-audit.csv\""))
                                .andExpect(header().string("Cache-Control", "no-store"))
                                .andExpect(content().string(csv));

                verify(queryService).findForExport(
                                ORGANIZATION_ID,
                                OrganizationStatusAuditAction.SUSPENDED,
                                "status-request-1",
                                "administrador",
                                OCCURRED_FROM,
                                OCCURRED_TO);
                verify(csvExporter).toCsv(events);
        }

        @Test
        @WithMockUser(username = "administrador", roles = "ADMIN")
        void deberiaResponder413SiSeSuperaElLimite() throws Exception {
                when(queryService.findForExport(
                                ORGANIZATION_ID,
                                null,
                                null,
                                null,
                                null,
                                null))
                                .thenThrow(new OrganizationStatusAuditExportLimitExceededException(1000));

                mockMvc.perform(get(
                                "/api/v1/organizations/{organizationId}/status-audit/export",
                                ORGANIZATION_ID))
                                .andExpect(status().is(413))
                                .andExpect(jsonPath("$.code")
                                                .value("ORGANIZATION_STATUS_AUDIT_EXPORT_LIMIT_EXCEEDED"))
                                .andExpect(jsonPath("$.errors.limit").value("1000"));

                verifyNoInteractions(csvExporter);
        }

        @Test
        @WithMockUser(username = "administrador", roles = "ADMIN")
        void deberiaRechazarUnRequestIdInvalido() throws Exception {
                mockMvc.perform(get(
                                "/api/v1/organizations/{organizationId}/status-audit/export",
                                ORGANIZATION_ID)
                                .param("requestId", "request id invalido"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                                .andExpect(jsonPath("$.errors.requestId")
                                                .value("El identificador de petición no tiene un formato válido"));

                verifyNoInteractions(queryService, csvExporter);
        }

        @Test
        @WithAnonymousUser
        void deberiaRequerirAutenticacion() throws Exception {
                mockMvc.perform(get(
                                "/api/v1/organizations/{organizationId}/status-audit/export",
                                ORGANIZATION_ID))
                                .andExpect(status().isUnauthorized());

                verifyNoInteractions(queryService, csvExporter);
        }

        @Test
        void analystNoPuedeExportarLaAuditoriaDeEstado() throws Exception {
                mockMvc.perform(get(
                                "/api/v1/organizations/{organizationId}/status-audit/export",
                                ORGANIZATION_ID))
                                .andExpect(status().isForbidden());

                verifyNoInteractions(queryService, csvExporter);
        }
}