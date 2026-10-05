package com.securefindings.organization.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.securefindings.api.error.GlobalExceptionHandler;
import com.securefindings.organization.application.OrganizationService;
import com.securefindings.organization.domain.Organization;
import com.securefindings.security.SecurityConfig;

@WebMvcTest(controllers = OrganizationController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class
})
@WithMockUser(username = "analista", roles = "ANALYST")
class OrganizationControllerTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private OrganizationService organizationService;

    private MockMvc mockMvc;

    @BeforeEach
    void configurarMockMvc() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void deberiaDevolverLaOrganizacionActual() throws Exception {
        Organization organization = new Organization(
                UUID.fromString(
                        "00000000-0000-0000-0000-000000000001"),
                "Secure Findings",
                "secure-findings",
                Instant.parse("2026-10-01T10:00:00Z"));

        when(organizationService.getCurrentOrganization())
                .thenReturn(organization);

        mockMvc.perform(get("/api/v1/organizations/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(organization.id().toString()))
                .andExpect(jsonPath("$.name")
                        .value("Secure Findings"))
                .andExpect(jsonPath("$.slug")
                        .value("secure-findings"))
                .andExpect(jsonPath("$.createdAt")
                        .value("2026-10-01T10:00:00Z"));

        verify(organizationService).getCurrentOrganization();
    }

    @Test
    @WithMockUser(username = "lector", roles = "READER")
    void deberiaRechazarUnRolSinPermiso() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/current"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(organizationService);
    }
}