package com.securefindings.organization.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.http.MediaType;
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

        private static final UUID ORGANIZATION_ID = UUID.fromString(
                        "00000000-0000-0000-0000-000000000001");

        private static final Instant CREATED_AT = Instant.parse(
                        "2026-10-01T10:00:00Z");

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
        @WithMockUser(username = "administrador", roles = "ADMIN")
        void deberiaListarOrganizacionesDeFormaPaginadaParaAdmin() throws Exception {
                Organization first = new Organization(
                                ORGANIZATION_ID,
                                "Alpha Security",
                                "alpha-security",
                                CREATED_AT);
                Organization second = new Organization(
                                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                                "Secure Findings",
                                "secure-findings",
                                CREATED_AT);

                when(organizationService.findOrganizations(0, 20))
                                .thenReturn(new PageImpl<>(
                                                List.of(first, second),
                                                PageRequest.of(0, 20),
                                                2));

                mockMvc.perform(get("/api/v1/organizations"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.content[0].name")
                                                .value("Alpha Security"))
                                .andExpect(jsonPath("$.content[1].id")
                                                .value(second.id().toString()))
                                .andExpect(jsonPath("$.page").value(0))
                                .andExpect(jsonPath("$.size").value(20))
                                .andExpect(jsonPath("$.totalElements").value(2))
                                .andExpect(jsonPath("$.totalPages").value(1))
                                .andExpect(jsonPath("$.first").value(true))
                                .andExpect(jsonPath("$.last").value(true));

                verify(organizationService).findOrganizations(0, 20);
        }

        @Test
        void deberiaRechazarAlAnalistaAlListarOrganizaciones() throws Exception {
                mockMvc.perform(get("/api/v1/organizations"))
                                .andExpect(status().isForbidden());

                verifyNoInteractions(organizationService);
        }

        @Test
        @WithMockUser(username = "administrador", roles = "ADMIN")
        void deberiaRechazarUnTamanoDePaginaSuperiorAlMaximo() throws Exception {
                mockMvc.perform(get("/api/v1/organizations")
                                .param("size", "101"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                                .andExpect(jsonPath("$.errors.size").exists());

                verifyNoInteractions(organizationService);
        }

        @Test
        void deberiaDevolverLaOrganizacionActual() throws Exception {
                Organization organization = new Organization(
                                ORGANIZATION_ID,
                                "Secure Findings",
                                "secure-findings",
                                CREATED_AT);

                when(organizationService.getCurrentOrganization())
                                .thenReturn(organization);

                mockMvc.perform(get("/api/v1/organizations/current"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id")
                                                .value(ORGANIZATION_ID.toString()))
                                .andExpect(jsonPath("$.name")
                                                .value("Secure Findings"))
                                .andExpect(jsonPath("$.slug")
                                                .value("secure-findings"))
                                .andExpect(jsonPath("$.createdAt")
                                                .value("2026-10-01T10:00:00Z"));

                verify(organizationService).getCurrentOrganization();
        }

        @Test
        @WithMockUser(username = "administrador", roles = "ADMIN")
        void deberiaPermitirAlAdminActualizarElNombre() throws Exception {
                Organization updated = new Organization(
                                ORGANIZATION_ID,
                                "Secure Findings Europe",
                                "secure-findings",
                                CREATED_AT);

                when(organizationService.updateCurrentOrganizationName(
                                "Secure Findings Europe"))
                                .thenReturn(updated);

                mockMvc.perform(patch("/api/v1/organizations/current")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "name": "Secure Findings Europe"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id")
                                                .value(ORGANIZATION_ID.toString()))
                                .andExpect(jsonPath("$.name")
                                                .value("Secure Findings Europe"))
                                .andExpect(jsonPath("$.slug")
                                                .value("secure-findings"))
                                .andExpect(jsonPath("$.createdAt")
                                                .value("2026-10-01T10:00:00Z"));

                verify(organizationService)
                                .updateCurrentOrganizationName(
                                                "Secure Findings Europe");
        }

        @Test
        void deberiaRechazarAlAnalistaAlActualizarElNombre() throws Exception {
                mockMvc.perform(patch("/api/v1/organizations/current")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "name": "Secure Findings Europe"
                                                }
                                                """))
                                .andExpect(status().isForbidden());

                verifyNoInteractions(organizationService);
        }

        @Test
        @WithMockUser(username = "administrador", roles = "ADMIN")
        void deberiaRechazarUnNombreVacio() throws Exception {
                mockMvc.perform(patch("/api/v1/organizations/current")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "name": " "
                                                }
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code")
                                                .value("VALIDATION_ERROR"))
                                .andExpect(jsonPath("$.errors.name")
                                                .value("El nombre de la organización es obligatorio"));

                verifyNoInteractions(organizationService);
        }
}