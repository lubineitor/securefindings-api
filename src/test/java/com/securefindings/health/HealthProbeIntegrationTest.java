package com.securefindings.health;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
class HealthProbeIntegrationTest {

        @SuppressWarnings("resource")
        @Container
        static final PostgreSQLContainer postgres = new PostgreSQLContainer(
                        "postgres:17-alpine")
                        .withDatabaseName("securefindings_health_test")
                        .withUsername("securefindings_health_test")
                        .withPassword("securefindings_health_test");

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

        @DynamicPropertySource
        static void registrarPropiedadesPostgres(
                        DynamicPropertyRegistry registry) {

                registry.add("spring.datasource.url", postgres::getJdbcUrl);
                registry.add("spring.datasource.username", postgres::getUsername);
                registry.add("spring.datasource.password", postgres::getPassword);
        }

        @Test
        void deberiaPublicarLivenessSinAutenticacionNiDetalles()
                        throws Exception {

                comprobarProbe("/actuator/health/liveness");
                comprobarProbe("/livez");
        }

        @Test
        void deberiaPublicarReadinessSinAutenticacionNiDetalles()
                        throws Exception {

                comprobarProbe("/actuator/health/readiness");
                comprobarProbe("/readyz");
        }

        @Test
        void deberiaProtegerElEstadoGlobalDeActuator()
                        throws Exception {

                mockMvc.perform(get("/actuator/health"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void deberiaRechazarMetricasPrometheusSinAutenticacion()
                        throws Exception {

                mockMvc.perform(get("/actuator/prometheus"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void deberiaRechazarMetricasPrometheusAUnAnalista()
                        throws Exception {

                mockMvc.perform(get("/actuator/prometheus")
                                .with(jwtAutenticado("analista", "ANALYST")))
                                .andExpect(status().isForbidden());
        }

        @Test
        void deberiaPermitirMetricasPrometheusAUnAdministrador()
                        throws Exception {

                mockMvc.perform(get("/actuator/prometheus")
                                .with(jwtAutenticado("administrador", "ADMIN")))
                                .andExpect(status().isOk())
                                .andExpect(content().string(containsString(
                                                "securefindings_rate_limit_requests_total")));
        }

        @Test
        void deberiaPermitirMetricasPrometheusAlRolDeSoloLectura()
                        throws Exception {

                mockMvc.perform(get("/actuator/prometheus")
                                .with(jwtAutenticado("prometheus", "METRICS_READER")))
                                .andExpect(status().isOk())
                                .andExpect(content().string(containsString(
                                                "securefindings_rate_limit_requests_total")));
        }

        @Test
        void deberiaRestringirElRolDeMetricasALosEndpointsDeObservabilidad()
                        throws Exception {

                mockMvc.perform(get("/api/v1/organizations/current")
                                .with(jwtAutenticado("prometheus", "METRICS_READER")))
                                .andExpect(status().isForbidden());
        }

        private RequestPostProcessor jwtAutenticado(
                        String username,
                        String role) {

                Jwt token = Jwt.withTokenValue("token-" + username)
                                .header("alg", "none")
                                .subject(username)
                                .claim("iss", "https://issuer.example.test/realms/securefindings")
                                .claim("preferred_username", username)
                                .build();

                return authentication(new JwtAuthenticationToken(
                                token,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role)),
                                username));
        }

        private void comprobarProbe(String path)
                        throws Exception {

                mockMvc.perform(get(path))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("UP"))
                                .andExpect(jsonPath("$.components").doesNotExist())
                                .andExpect(jsonPath("$.details").doesNotExist());
        }
}