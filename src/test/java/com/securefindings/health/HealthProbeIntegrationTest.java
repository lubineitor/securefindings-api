package com.securefindings.health;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class HealthProbeIntegrationTest {

    @SuppressWarnings("resource")
    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer(
            "postgres:17-alpine")
            .withDatabaseName("securefindings_health_test")
            .withUsername("securefindings_health_test")
            .withPassword("securefindings_health_test");

    @Autowired
    private MockMvc mockMvc;

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

    private void comprobarProbe(String path)
            throws Exception {

        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
    }
}