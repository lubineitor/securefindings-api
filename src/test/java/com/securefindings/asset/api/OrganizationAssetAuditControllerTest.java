package com.securefindings.asset.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.securefindings.api.error.GlobalExceptionHandler;
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
class OrganizationAssetAuditControllerTest {

    private static final UUID ASSET_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000101");

    private static final UUID EVENT_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000201");

    private static final Instant OCCURRED_FROM = Instant.parse("2026-10-01T00:00:00Z");

    private static final Instant OCCURRED_TO = Instant.parse("2026-10-31T23:59:59Z");

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private AssetAuditQueryService assetAuditQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void configurarMockMvc() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void deberiaConsultarLaAuditoriaDeLaOrganizacionActual() throws Exception {
        AssetAuditEntity event = mock(AssetAuditEntity.class);

        when(event.getId()).thenReturn(EVENT_ID);
        when(event.getAssetId()).thenReturn(ASSET_ID);
        when(event.getAction()).thenReturn(AssetAuditAction.ARCHIVED);
        when(event.getActor()).thenReturn("analista");
        when(event.getOccurredAt())
                .thenReturn(Instant.parse("2026-10-10T08:00:00Z"));
        when(event.getRequestId()).thenReturn("asset-request-123");

        when(assetAuditQueryService.findPageByOrganization(
                1,
                10,
                AssetAuditAction.ARCHIVED,
                "asset-request-123",
                "analista",
                OCCURRED_FROM,
                OCCURRED_TO))
                .thenReturn(new PageImpl<>(
                        List.of(event),
                        PageRequest.of(1, 10),
                        11));

        mockMvc.perform(get("/api/v1/organizations/current/audit/assets")
                .param("page", "1")
                .param("size", "10")
                .param("action", "ARCHIVED")
                .param("requestId", "asset-request-123")
                .param("actor", "analista")
                .param("occurredFrom", "2026-10-01T00:00:00Z")
                .param("occurredTo", "2026-10-31T23:59:59Z")
                .param("organizationId", "00000000-0000-0000-0000-000000000999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].assetId").value(ASSET_ID.toString()))
                .andExpect(jsonPath("$.content[0].action").value("ARCHIVED"))
                .andExpect(jsonPath("$.content[0].actor").value("analista"))
                .andExpect(jsonPath("$.content[0].requestId")
                        .value("asset-request-123"))
                .andExpect(jsonPath("$.content[0].organizationId").doesNotExist())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(11));

        verify(assetAuditQueryService).findPageByOrganization(
                1,
                10,
                AssetAuditAction.ARCHIVED,
                "asset-request-123",
                "analista",
                OCCURRED_FROM,
                OCCURRED_TO);
    }

    @Test
    void deberiaRechazarUnRequestIdInvalido() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/current/audit/assets")
                .param("requestId", "request id invalido"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.requestId")
                        .value("El identificador de petición no tiene un formato válido"));

        verifyNoInteractions(assetAuditQueryService);
    }

    @Test
    void deberiaRechazarUnTamanoSuperiorAlMaximo() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/current/audit/assets")
                .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(assetAuditQueryService);
    }

    @Test
    @WithMockUser(username = "usuario", roles = "USER")
    void deberiaRechazarUnRolSinPermiso() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/current/audit/assets"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(assetAuditQueryService);
    }

    @Test
    @WithAnonymousUser
    void deberiaRequerirAutenticacion() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/current/audit/assets"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(assetAuditQueryService);
    }

    @Test
    void deberiaRechazarUnaAccionInvalida() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/current/audit/assets")
                .param("action", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.action").exists());

        verifyNoInteractions(assetAuditQueryService);
    }
}