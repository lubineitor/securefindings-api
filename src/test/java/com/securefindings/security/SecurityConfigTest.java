package com.securefindings.security;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.securefindings.health.HealthController;

@WebMvcTest(controllers = HealthController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void configurarMockMvc() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void deberiaPermitirAccesoAlHealthSinAutenticacion() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk());
    }

    @Test
    void deberiaRechazarAccesoAFindingsSinAutenticacion() throws Exception {
        mockMvc.perform(get("/api/v1/findings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "analista", roles = "ANALYST")
    void analistaNoPuedeEliminarUnHallazgo() throws Exception {
        mockMvc.perform(delete("/api/v1/findings/" + UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    void deberiaDevolverUnaRespuestaJson401SinAutenticacion()
            throws Exception {

        mockMvc.perform(get("/api/v1/findings")
                .with(anonymous()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code")
                        .value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message")
                        .value(
                                "La autenticación es necesaria "
                                        + "para acceder a este recurso"))
                .andExpect(jsonPath("$.errors")
                        .isEmpty());
    }

    @Test
    @WithMockUser(username = "analista", roles = "ANALYST")
    void deberiaDevolverUnaRespuestaJson403SiElAnalistaElimina()
            throws Exception {

        UUID findingId = UUID.randomUUID();

        mockMvc.perform(delete(
                "/api/v1/findings/{id}",
                findingId))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code")
                        .value("FORBIDDEN"))
                .andExpect(jsonPath("$.message")
                        .value(
                                "El usuario no tiene permisos "
                                        + "para acceder a este recurso"))
                .andExpect(jsonPath("$.errors")
                        .isEmpty());
    }
}