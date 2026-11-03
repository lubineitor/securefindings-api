package com.securefindings.asset.api;

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
import com.securefindings.asset.application.AssetAuditExportLimitExceededException;
import com.securefindings.asset.application.AssetAuditQueryService;
import com.securefindings.asset.domain.AssetAuditAction;
import com.securefindings.asset.persistence.AssetAuditEntity;
import com.securefindings.security.SecurityConfig;

@WebMvcTest(controllers = OrganizationAssetAuditController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class
})
@WithMockUser(username = "analista", roles = "ANALYST")
class OrganizationAssetAuditExportControllerTest {

    private static final UUID EVENT_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000201");

    private static final UUID ASSET_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000101");

    private static final Instant OCCURRED_FROM = Instant.parse("2026-10-01T00:00:00Z");

    private static final Instant OCCURRED_TO = Instant.parse("2026-10-31T23:59:59Z");

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private AssetAuditQueryService assetAuditQueryService;

    @MockitoBean
    private AssetAuditCsvExporter assetAuditCsvExporter;

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
        AssetAuditEntity event = mock(AssetAuditEntity.class);
        List<AssetAuditEntity> events = List.of(event);
        String csv = "\"id\",\"assetId\",\"action\",\"actor\",\"occurredAt\",\"requestId\"\r\n"
                + "\"00000000-0000-0000-0000-000000000201\","
                + "\"00000000-0000-0000-0000-000000000101\","
                + "\"ARCHIVED\",\"analista\",\"2026-10-10T08:00:00Z\","
                + "\"asset-request-123\"\r\n";

        when(event.getId()).thenReturn(EVENT_ID);
        when(event.getAssetId()).thenReturn(ASSET_ID);
        when(event.getAction()).thenReturn(AssetAuditAction.ARCHIVED);
        when(event.getActor()).thenReturn("analista");
        when(event.getOccurredAt())
                .thenReturn(Instant.parse("2026-10-10T08:00:00Z"));
        when(event.getRequestId()).thenReturn("asset-request-123");

        when(assetAuditQueryService.findForOrganizationExport(
                AssetAuditAction.ARCHIVED,
                "asset-request-123",
                "analista",
                OCCURRED_FROM,
                OCCURRED_TO))
                .thenReturn(events);

        when(assetAuditCsvExporter.toCsv(events)).thenReturn(csv);

        mockMvc.perform(get(
                "/api/v1/organizations/current/audit/assets/export")
                .param("action", "ARCHIVED")
                .param("requestId", "asset-request-123")
                .param("actor", "analista")
                .param("occurredFrom", "2026-10-01T00:00:00Z")
                .param("occurredTo", "2026-10-31T23:59:59Z"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.parseMediaType("text/csv")))
                .andExpect(header().string(
                        "Content-Disposition",
                        "attachment; filename=\"securefindings-asset-audit.csv\""))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(csv));

        verify(assetAuditQueryService).findForOrganizationExport(
                AssetAuditAction.ARCHIVED,
                "asset-request-123",
                "analista",
                OCCURRED_FROM,
                OCCURRED_TO);

        verify(assetAuditCsvExporter).toCsv(events);
    }

    @Test
    void deberiaResponder413SiLaExportacionSuperaElLimite() throws Exception {
        when(assetAuditQueryService.findForOrganizationExport(
                null,
                null,
                null,
                null,
                null))
                .thenThrow(new AssetAuditExportLimitExceededException(
                        AssetAuditQueryService.MAX_EXPORT_ROWS));

        mockMvc.perform(get(
                "/api/v1/organizations/current/audit/assets/export"))
                .andExpect(status().is(413))
                .andExpect(jsonPath("$.code")
                        .value("ASSET_AUDIT_EXPORT_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.errors.limit").value("1000"));

        verifyNoInteractions(assetAuditCsvExporter);
    }

    @Test
    void deberiaRechazarUnRequestIdInvalido() throws Exception {
        mockMvc.perform(get(
                "/api/v1/organizations/current/audit/assets/export")
                .param("requestId", "request id invalido"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.requestId")
                        .value("El identificador de petición no tiene un formato válido"));

        verifyNoInteractions(assetAuditQueryService, assetAuditCsvExporter);
    }

    @Test
    @WithAnonymousUser
    void deberiaRequerirAutenticacion() throws Exception {
        mockMvc.perform(get(
                "/api/v1/organizations/current/audit/assets/export"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(assetAuditQueryService, assetAuditCsvExporter);
    }

    @Test
    @WithMockUser(username = "usuario", roles = "USER")
    void deberiaRechazarUnRolSinPermiso() throws Exception {
        mockMvc.perform(get(
                "/api/v1/organizations/current/audit/assets/export"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(assetAuditQueryService, assetAuditCsvExporter);
    }
}