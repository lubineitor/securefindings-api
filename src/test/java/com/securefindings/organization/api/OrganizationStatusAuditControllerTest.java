package com.securefindings.organization.api;

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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.securefindings.api.error.GlobalExceptionHandler;
import com.securefindings.organization.application.OrganizationStatusAuditQueryService;
import com.securefindings.organization.domain.OrganizationStatusAuditAction;
import com.securefindings.organization.domain.OrganizationStatusAuditEvent;
import com.securefindings.organization.persistence.OrganizationStatusAuditEntity;
import com.securefindings.security.SecurityConfig;

@WebMvcTest(controllers = OrganizationStatusAuditController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
@WithMockUser(username = "analista", roles = "ANALYST")
class OrganizationStatusAuditControllerTest {

    private static final UUID ORGANIZATION_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000001");

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private OrganizationStatusAuditQueryService queryService;

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
    void adminPuedeConsultarLaAuditoriaDeEstado() throws Exception {
        OrganizationStatusAuditEvent event = new OrganizationStatusAuditEvent(
                UUID.fromString("00000000-0000-0000-0000-000000000401"),
                ORGANIZATION_ID,
                OrganizationStatusAuditAction.SUSPENDED,
                "administrador",
                Instant.parse("2026-11-23T10:00:00Z"),
                "request-401");

        OrganizationStatusAuditEntity entity = new OrganizationStatusAuditEntity(event);

        when(queryService.findPageByOrganizationId(
                ORGANIZATION_ID,
                0,
                20))
                .thenReturn(new PageImpl<>(
                        List.of(entity),
                        PageRequest.of(0, 20),
                        1));

        mockMvc.perform(get(
                "/api/v1/organizations/{organizationId}/status-audit",
                ORGANIZATION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].organizationId")
                        .value(ORGANIZATION_ID.toString()))
                .andExpect(jsonPath("$.content[0].action")
                        .value("SUSPENDED"))
                .andExpect(jsonPath("$.content[0].actor")
                        .value("administrador"))
                .andExpect(jsonPath("$.content[0].requestId")
                        .value("request-401"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(queryService).findPageByOrganizationId(
                ORGANIZATION_ID,
                0,
                20);
    }

    @Test
    void analystNoPuedeConsultarLaAuditoriaDeEstado() throws Exception {
        mockMvc.perform(get(
                "/api/v1/organizations/{organizationId}/status-audit",
                ORGANIZATION_ID))
                .andExpect(status().isForbidden());

        verifyNoInteractions(queryService);
    }

    @Test
    @WithMockUser(username = "administrador", roles = "ADMIN")
    void rechazaUnTamanoDePaginaSuperiorAlMaximo() throws Exception {
        mockMvc.perform(get(
                "/api/v1/organizations/{organizationId}/status-audit",
                ORGANIZATION_ID)
                .param("size", "101"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(queryService);
    }
}