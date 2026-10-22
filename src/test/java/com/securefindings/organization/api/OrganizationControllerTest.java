package com.securefindings.organization.api;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.securefindings.api.error.GlobalExceptionHandler;
import com.securefindings.organization.application.OrganizationNotFoundException;
import com.securefindings.organization.application.OrganizationSlugAlreadyExistsException;
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

                when(organizationService.findOrganizations(0, 20, null))
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

                verify(organizationService).findOrganizations(0, 20, null);
        }

        @Test
        @WithMockUser(username = "administrador", roles = "ADMIN")
        void deberiaFiltrarOrganizacionesPorNombreOSlug() throws Exception {
                Organization organization = new Organization(
                                ORGANIZATION_ID,
                                "Acme Security",
                                "acme-security",
                                CREATED_AT);

                when(organizationService.findOrganizations(1, 10, "acme"))
                                .thenReturn(new PageImpl<>(
                                                List.of(organization),
                                                PageRequest.of(1, 10),
                                                11));

                mockMvc.perform(get("/api/v1/organizations")
                                .param("q", "acme")
                                .param("page", "1")
                                .param("size", "10"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[0].slug")
                                                .value("acme-security"))
                                .andExpect(jsonPath("$.page").value(1))
                                .andExpect(jsonPath("$.size").value(10))
                                .andExpect(jsonPath("$.totalElements").value(11));

                verify(organizationService).findOrganizations(1, 10, "acme");
        }

        @Test
        @WithMockUser(username = "administrador", roles = "ADMIN")
        void deberiaRechazarUnaBusquedaDeOrganizacionesSuperiorAlMaximo()
                        throws Exception {

                mockMvc.perform(get("/api/v1/organizations")
                                .param("q", "a".repeat(101)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code")
                                                .value("VALIDATION_ERROR"))
                                .andExpect(jsonPath("$.errors.q").exists());

                verifyNoInteractions(organizationService);
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
        @WithMockUser(username = "administrador", roles = "ADMIN")
        void deberiaRechazarLosLimitesInferioresDeLaPaginacion() throws Exception {
                mockMvc.perform(get("/api/v1/organizations")
                                .param("page", "-1"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                                .andExpect(jsonPath("$.errors.page").exists());

                mockMvc.perform(get("/api/v1/organizations")
                                .param("size", "0"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                                .andExpect(jsonPath("$.errors.size").exists());

                verifyNoInteractions(organizationService);
        }

        @Test
        @WithMockUser(username = "administrador", roles = "ADMIN")
        void deberiaConsultarUnaOrganizacionPorIdentificadorParaAdmin() throws Exception {
                Organization organization = new Organization(
                                ORGANIZATION_ID,
                                "Secure Findings",
                                "secure-findings",
                                CREATED_AT);

                when(organizationService.getOrganization(ORGANIZATION_ID))
                                .thenReturn(organization);

                mockMvc.perform(get("/api/v1/organizations/{id}", ORGANIZATION_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id")
                                                .value(ORGANIZATION_ID.toString()))
                                .andExpect(jsonPath("$.name")
                                                .value("Secure Findings"))
                                .andExpect(jsonPath("$.slug")
                                                .value("secure-findings"));

                verify(organizationService).getOrganization(ORGANIZATION_ID);
        }

        @Test
        void deberiaRechazarAlAnalistaAlConsultarUnaOrganizacionPorIdentificador()
                        throws Exception {

                mockMvc.perform(get("/api/v1/organizations/{id}", ORGANIZATION_ID))
                                .andExpect(status().isForbidden());

                verifyNoInteractions(organizationService);
        }

        @Test
        @WithMockUser(username = "administrador", roles = "ADMIN")
        void deberiaDevolver404SiLaOrganizacionSolicitadaNoExiste() throws Exception {
                when(organizationService.getOrganization(ORGANIZATION_ID))
                                .thenThrow(new OrganizationNotFoundException(
                                                ORGANIZATION_ID));

                mockMvc.perform(get("/api/v1/organizations/{id}", ORGANIZATION_ID))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.code")
                                                .value("ORGANIZATION_NOT_FOUND"));
        }

        @Test
        @WithMockUser(username = "administrador", roles = "ADMIN")
        void deberiaRechazarUnIdentificadorQueNoSeaUuid() throws Exception {
                mockMvc.perform(get("/api/v1/organizations/{id}", "uuid-invalido"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code")
                                                .value("VALIDATION_ERROR"));

                verifyNoInteractions(organizationService);
        }

        @Test
        @WithMockUser(username = "administrador", roles = "ADMIN")
        void deberiaActualizarUnaOrganizacionPorIdentificador() throws Exception {
                Organization updated = new Organization(
                                ORGANIZATION_ID,
                                "Secure Findings Europe",
                                "secure-findings",
                                CREATED_AT);

                when(organizationService.updateOrganization(
                                ORGANIZATION_ID,
                                "Secure Findings Europe"))
                                .thenReturn(updated);

                mockMvc.perform(patch("/api/v1/organizations/{id}", ORGANIZATION_ID)
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

                verify(organizationService).updateOrganization(
                                ORGANIZATION_ID,
                                "Secure Findings Europe");
        }

        @Test
        void deberiaRechazarAlAnalistaAlActualizarUnaOrganizacionPorIdentificador()
                        throws Exception {

                mockMvc.perform(patch("/api/v1/organizations/{id}", ORGANIZATION_ID)
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
        void deberiaDevolver404AlActualizarUnaOrganizacionInexistente()
                        throws Exception {

                when(organizationService.updateOrganization(
                                ORGANIZATION_ID,
                                "Secure Findings Europe"))
                                .thenThrow(new OrganizationNotFoundException(
                                                ORGANIZATION_ID));

                mockMvc.perform(patch("/api/v1/organizations/{id}", ORGANIZATION_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "name": "Secure Findings Europe"
                                                }
                                                """))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.code")
                                                .value("ORGANIZATION_NOT_FOUND"));
        }

        @Test
        @WithMockUser(username = "administrador", roles = "ADMIN")
        void deberiaCrearOrganizacionYDevolverLocation() throws Exception {
                Organization created = new Organization(
                                UUID.fromString("00000000-0000-0000-0000-000000000010"),
                                "Acme Security",
                                "acme-security",
                                CREATED_AT);

                when(organizationService.createOrganization(
                                "Acme Security",
                                "acme-security"))
                                .thenReturn(created);

                mockMvc.perform(post("/api/v1/organizations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "name": "Acme Security",
                                                    "slug": "acme-security"
                                                }
                                                """))
                                .andExpect(status().isCreated())
                                .andExpect(header().string(
                                                "Location",
                                                endsWith("/api/v1/organizations/" + created.id())))
                                .andExpect(jsonPath("$.id")
                                                .value(created.id().toString()))
                                .andExpect(jsonPath("$.name")
                                                .value("Acme Security"))
                                .andExpect(jsonPath("$.slug")
                                                .value("acme-security"));

                verify(organizationService).createOrganization(
                                "Acme Security",
                                "acme-security");
        }

        @Test
        void deberiaRechazarAlAnalistaAlCrearUnaOrganizacion() throws Exception {
                mockMvc.perform(post("/api/v1/organizations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "name": "Acme Security",
                                                    "slug": "acme-security"
                                                }
                                                """))
                                .andExpect(status().isForbidden());

                verifyNoInteractions(organizationService);
        }

        @Test
        @WithMockUser(username = "administrador", roles = "ADMIN")
        void deberiaRechazarUnSlugConFormatoInvalido() throws Exception {
                mockMvc.perform(post("/api/v1/organizations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "name": "Acme Security",
                                                    "slug": "Acme Security"
                                                }
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code")
                                                .value("VALIDATION_ERROR"))
                                .andExpect(jsonPath("$.errors.slug").exists());

                verifyNoInteractions(organizationService);
        }

        @Test
        @WithMockUser(username = "administrador", roles = "ADMIN")
        void deberiaRechazarNombreYSlugEnBlanco() throws Exception {
                mockMvc.perform(post("/api/v1/organizations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "name": "   ",
                                                    "slug": "   "
                                                }
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code")
                                                .value("VALIDATION_ERROR"))
                                .andExpect(jsonPath("$.errors.name").exists())
                                .andExpect(jsonPath("$.errors.slug").exists());

                verifyNoInteractions(organizationService);
        }

        @Test
        @WithMockUser(username = "administrador", roles = "ADMIN")
        void deberiaRechazarNombreYSlugQueSuperanSuLongitudMaxima() throws Exception {
                String name = "a".repeat(151);
                String slug = "a".repeat(101);

                mockMvc.perform(post("/api/v1/organizations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "name": "%s",
                                                    "slug": "%s"
                                                }
                                                """.formatted(name, slug)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code")
                                                .value("VALIDATION_ERROR"))
                                .andExpect(jsonPath("$.errors.name").exists())
                                .andExpect(jsonPath("$.errors.slug").exists());

                verifyNoInteractions(organizationService);
        }

        @Test
        @WithMockUser(username = "administrador", roles = "ADMIN")
        void deberiaDevolver409SiElSlugYaEstaEnUso() throws Exception {
                when(organizationService.createOrganization(
                                "Acme Security",
                                "acme-security"))
                                .thenThrow(new OrganizationSlugAlreadyExistsException(
                                                "acme-security"));

                mockMvc.perform(post("/api/v1/organizations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "name": "Acme Security",
                                                    "slug": "acme-security"
                                                }
                                                """))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.code")
                                                .value("ORGANIZATION_SLUG_ALREADY_EXISTS"))
                                .andExpect(jsonPath("$.errors.slug")
                                                .value("acme-security"));
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
