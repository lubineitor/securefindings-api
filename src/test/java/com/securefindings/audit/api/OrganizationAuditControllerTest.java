package com.securefindings.audit.api;

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
class OrganizationAuditControllerTest {

    private static final UUID ORGANIZATION_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000001");
    private static final UUID FINDING_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000101");

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private AuditService auditService;

    private MockMvc mockMvc;

    @BeforeEach
    void configurarMockMvc() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void deberiaConsultarAuditoriaPaginadaDeLaOrganizacionActual() throws Exception {
        Instant occurredFrom = Instant.parse("2026-10-01T00:00:00Z");
        Instant occurredTo = Instant.parse("2026-10-31T23:59:59Z");
        FindingAuditEntity event = new FindingAuditEntity(
                UUID.randomUUID(),
                FINDING_ID,
                ORGANIZATION_ID,
                AuditAction.UPDATED,
                "analista",
                Instant.parse("2026-10-10T08:00:00Z"),
                "audit-request-123");

        when(auditService.findPageByOrganization(
                1,
                10,
                AuditAction.UPDATED,
                "audit-request-123",
                "analista",
                occurredFrom,
                occurredTo))
                .thenReturn(new PageImpl<>(List.of(event), PageRequest.of(1, 10), 11));

        mockMvc.perform(get("/api/v1/organizations/current/audit")
                .param("page", "1")
                .param("size", "10")
                .param("action", "UPDATED")
                .param("requestId", "audit-request-123")
                .param("actor", "analista")
                .param("occurredFrom", "2026-10-01T00:00:00Z")
                .param("occurredTo", "2026-10-31T23:59:59Z")
                .param("organizationId", "00000000-0000-0000-0000-000000000999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].findingId").value(FINDING_ID.toString()))
                .andExpect(jsonPath("$.content[0].action").value("UPDATED"))
                .andExpect(jsonPath("$.content[0].actor").value("analista"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(11));

        verify(auditService).findPageByOrganization(
                1,
                10,
                AuditAction.UPDATED,
                "audit-request-123",
                "analista",
                occurredFrom,
                occurredTo);
    }

    @Test
    void deberiaRechazarUnRequestIdInvalido() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/current/audit")
                .param("requestId", "request id invalido"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.requestId")
                        .value("El identificador de petición no tiene un formato válido"));

        verifyNoInteractions(auditService);
    }

    @Test
    void deberiaRechazarUnTamanoDePaginaSuperiorAlMaximo() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/current/audit")
                .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(auditService);
    }

    @Test
    @WithMockUser(username = "usuario", roles = "USER")
    void deberiaRechazarUnRolSinPermisoParaConsultarAuditoria() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/current/audit"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(auditService);
    }

    @Test
    @WithAnonymousUser
    void deberiaRequerirAutenticacionParaConsultarAuditoria() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/current/audit"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(auditService);
    }

    @Test
    void deberiaRechazarUnaAccionDeAuditoriaInvalida() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/current/audit")
                .param("action", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.action").exists());

        verifyNoInteractions(auditService);
    }
}