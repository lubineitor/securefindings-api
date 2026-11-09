package com.securefindings.audit.api;

import static org.mockito.Mockito.mock;
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
import com.securefindings.audit.application.AuditExportLimitExceededException;
import com.securefindings.audit.application.AuditService;
import com.securefindings.audit.domain.AuditAction;
import com.securefindings.audit.persistence.FindingAuditEntity;
import com.securefindings.security.SecurityConfig;

@WebMvcTest(controllers = OrganizationAuditController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class
})
@WithMockUser(username = "analista", roles = "ANALYST")
class OrganizationAuditExportControllerTest {

    private static final UUID FINDING_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000101");
    private static final Instant OCCURRED_FROM = Instant.parse("2026-10-01T00:00:00Z");
    private static final Instant OCCURRED_TO = Instant.parse("2026-10-31T23:59:59Z");

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean
    private FindingAuditCsvExporter findingAuditCsvExporter;

    private MockMvc mockMvc;

    @BeforeEach
    void configurarMockMvc() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void deberiaExportarCsvAplicandoFiltros() throws Exception {
        FindingAuditEntity event = mock(FindingAuditEntity.class);
        List<FindingAuditEntity> events = List.of(event);
        String csv = "\"id\",\"findingId\",\"action\",\"actor\",\"occurredAt\",\"requestId\"\r\n"
                + "\"11111111-1111-1111-1111-111111111111\","
                + "\"00000000-0000-0000-0000-000000000101\","
                + "\"UPDATED\",\"analista\",\"2026-10-10T08:00:00Z\","
                + "\"audit-request-123\"\r\n";

        when(event.getId()).thenReturn(
                UUID.fromString("11111111-1111-1111-1111-111111111111"));
        when(event.getFindingId()).thenReturn(FINDING_ID);
        when(event.getAction()).thenReturn(AuditAction.UPDATED);
        when(event.getActor()).thenReturn("analista");
        when(event.getOccurredAt())
                .thenReturn(Instant.parse("2026-10-10T08:00:00Z"));
        when(event.getRequestId()).thenReturn("audit-request-123");

        when(auditService.findForOrganizationExport(
                AuditAction.UPDATED,
                "audit-request-123",
                "analista",
                OCCURRED_FROM,
                OCCURRED_TO))
                .thenReturn(events);
        when(findingAuditCsvExporter.toCsv(events)).thenReturn(csv);

        mockMvc.perform(get("/api/v1/organizations/current/audit/export")
                .param("action", "UPDATED")
                .param("requestId", "audit-request-123")
                .param("actor", "analista")
                .param("occurredFrom", "2026-10-01T00:00:00Z")
                .param("occurredTo", "2026-10-31T23:59:59Z"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.parseMediaType("text/csv")))
                .andExpect(header().string(
                        "Content-Disposition",
                        "attachment; filename=\"securefindings-finding-audit.csv\""))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(csv));

        verify(auditService).findForOrganizationExport(
                AuditAction.UPDATED,
                "audit-request-123",
                "analista",
                OCCURRED_FROM,
                OCCURRED_TO);
        verify(findingAuditCsvExporter).toCsv(events);
    }

    @Test
    void deberiaResponder413SiLaExportacionSuperaElLimite() throws Exception {
        when(auditService.findForOrganizationExport(
                null,
                null,
                null,
                null,
                null))
                .thenThrow(new AuditExportLimitExceededException(
                        AuditService.MAX_EXPORT_ROWS));

        mockMvc.perform(get("/api/v1/organizations/current/audit/export"))
                .andExpect(status().is(413))
                .andExpect(jsonPath("$.code")
                        .value("AUDIT_EXPORT_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.errors.limit").value("1000"));

        verifyNoInteractions(findingAuditCsvExporter);
    }

    @Test
    void deberiaRechazarUnRequestIdInvalido() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/current/audit/export")
                .param("requestId", "request id invalido"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(auditService, findingAuditCsvExporter);
    }

    @Test
    void deberiaRechazarUnRangoDeFechasInvertido() throws Exception {
        when(auditService.findForOrganizationExport(
                null,
                null,
                null,
                OCCURRED_TO,
                OCCURRED_FROM))
                .thenThrow(new IllegalArgumentException(
                        "occurredFrom debe ser anterior o igual a occurredTo"));

        mockMvc.perform(get("/api/v1/organizations/current/audit/export")
                .param("occurredFrom", "2026-10-31T23:59:59Z")
                .param("occurredTo", "2026-10-01T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(username = "usuario", roles = "USER")
    void deberiaRechazarUnRolSinPermiso() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/current/audit/export"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(auditService, findingAuditCsvExporter);
    }

    @Test
    @WithAnonymousUser
    void deberiaRequerirAutenticacion() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/current/audit/export"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(auditService, findingAuditCsvExporter);
    }
}