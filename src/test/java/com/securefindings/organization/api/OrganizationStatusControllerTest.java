package com.securefindings.organization.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.securefindings.api.error.GlobalExceptionHandler;
import com.securefindings.organization.application.OrganizationLifecycleService;
import com.securefindings.organization.domain.Organization;
import com.securefindings.organization.domain.OrganizationStatus;
import com.securefindings.security.SecurityConfig;

@WebMvcTest(controllers = OrganizationStatusController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
@WithMockUser(username = "analista", roles = "ANALYST")
class OrganizationStatusControllerTest {

        private static final UUID ORGANIZATION_ID = UUID.fromString(
                        "00000000-0000-0000-0000-000000000001");

        private static final String SUSPENSION_REASON = "Incidente confirmado por revisión";

        private static final String REACTIVATION_REASON = "Remediación validada por el administrador";

        @Autowired
        private WebApplicationContext context;

        @MockitoBean
        private OrganizationLifecycleService lifecycleService;

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
        void adminPuedeSuspenderUnaOrganizacion() throws Exception {
                Organization suspended = new Organization(
                                ORGANIZATION_ID,
                                "Secure Findings",
                                "secure-findings",
                                Instant.parse("2026-10-01T10:00:00Z"),
                                OrganizationStatus.SUSPENDED);

                when(lifecycleService.changeStatus(
                                ORGANIZATION_ID,
                                OrganizationStatus.SUSPENDED,
                                SUSPENSION_REASON))
                                .thenReturn(suspended);

                mockMvc.perform(patch(
                                "/api/v1/organizations/{organizationId}/status",
                                ORGANIZATION_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "status": "SUSPENDED",
                                                  "reason": "Incidente confirmado por revisión"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(ORGANIZATION_ID.toString()))
                                .andExpect(jsonPath("$.status").value("SUSPENDED"));

                verify(lifecycleService).changeStatus(
                                ORGANIZATION_ID,
                                OrganizationStatus.SUSPENDED,
                                SUSPENSION_REASON);
        }

        @Test
        @WithMockUser(username = "administrador", roles = "ADMIN")
        void adminPuedeReactivarUnaOrganizacionSuspendida() throws Exception {
                Organization active = new Organization(
                                ORGANIZATION_ID,
                                "Secure Findings",
                                "secure-findings",
                                Instant.parse("2026-10-01T10:00:00Z"),
                                OrganizationStatus.ACTIVE);

                when(lifecycleService.changeStatus(
                                ORGANIZATION_ID,
                                OrganizationStatus.ACTIVE,
                                REACTIVATION_REASON))
                                .thenReturn(active);

                mockMvc.perform(patch(
                                "/api/v1/organizations/{organizationId}/status",
                                ORGANIZATION_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "status": "ACTIVE",
                                                  "reason": "Remediación validada por el administrador"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(ORGANIZATION_ID.toString()))
                                .andExpect(jsonPath("$.status").value("ACTIVE"));

                verify(lifecycleService).changeStatus(
                                ORGANIZATION_ID,
                                OrganizationStatus.ACTIVE,
                                REACTIVATION_REASON);
        }

        @Test
        void analystNoPuedeCambiarElEstadoDeUnaOrganizacion() throws Exception {
                mockMvc.perform(patch(
                                "/api/v1/organizations/{organizationId}/status",
                                ORGANIZATION_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "status": "SUSPENDED",
                                                  "reason": "Incidente confirmado por revisión"
                                                }
                                                """))
                                .andExpect(status().isForbidden());

                verifyNoInteractions(lifecycleService);
        }

        @Test
        @WithMockUser(username = "administrador", roles = "ADMIN")
        void deberiaValidarQueElEstadoEsteInformado() throws Exception {
                mockMvc.perform(patch(
                                "/api/v1/organizations/{organizationId}/status",
                                ORGANIZATION_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "status": null,
                                                  "reason": "Incidente confirmado por revisión"
                                                }
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

                verifyNoInteractions(lifecycleService);
        }

        @Test
        @WithMockUser(username = "administrador", roles = "ADMIN")
        void deberiaRechazarLaPeticionSiFaltaElMotivo() throws Exception {
                mockMvc.perform(patch(
                                "/api/v1/organizations/{organizationId}/status",
                                ORGANIZATION_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "status": "SUSPENDED"
                                                }
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

                verifyNoInteractions(lifecycleService);
        }

        @Test
        @WithMockUser(username = "administrador", roles = "ADMIN")
        void deberiaRechazarUnMotivoDemasiadoCorto() throws Exception {
                mockMvc.perform(patch(
                                "/api/v1/organizations/{organizationId}/status",
                                ORGANIZATION_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "status": "SUSPENDED",
                                                  "reason": "fallo"
                                                }
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

                verifyNoInteractions(lifecycleService);
        }

        @Test
        @WithMockUser(username = "administrador", roles = "ADMIN")
        void deberiaRechazarCincoEmojisComoMotivoDemasiadoCorto() throws Exception {
                mockMvc.perform(patch(
                                "/api/v1/organizations/{organizationId}/status",
                                ORGANIZATION_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "status": "SUSPENDED",
                                                  "reason": "😀😀😀😀😀"
                                                }
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                                .andExpect(jsonPath("$.errors.reason")
                                                .value("El motivo debe tener entre 10 y 500 caracteres"));

                verifyNoInteractions(lifecycleService);
        }
}