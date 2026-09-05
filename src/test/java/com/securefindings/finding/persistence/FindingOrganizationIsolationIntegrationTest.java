package com.securefindings.finding.persistence;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.securefindings.finding.application.FindingNotFoundException;
import com.securefindings.finding.application.FindingService;
import com.securefindings.finding.domain.Finding;
import com.securefindings.finding.domain.FindingSeverity;

@Testcontainers
@SpringBootTest
class FindingOrganizationIsolationIntegrationTest {

    private static final UUID ORGANIZATION_A = UUID.fromString(
            "00000000-0000-0000-0000-000000000001");

    private static final UUID ORGANIZATION_B = UUID.fromString(
            "00000000-0000-0000-0000-000000000002");

    @SuppressWarnings("resource")
    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("securefindings_test")
            .withUsername("securefindings_test")
            .withPassword("securefindings_test");

    @DynamicPropertySource
    static void registerPostgresProperties(
            DynamicPropertyRegistry registry) {

        registry.add(
                "spring.datasource.url",
                postgres::getJdbcUrl);

        registry.add(
                "spring.datasource.username",
                postgres::getUsername);

        registry.add(
                "spring.datasource.password",
                postgres::getPassword);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FindingService findingService;

    @Autowired
    private FindingRepository findingRepository;

    @BeforeEach
    void prepararSegundaOrganizacion() {
        findingRepository.deleteAll();

        jdbcTemplate.update(
                """
                        INSERT INTO organizations (
                            id,
                            name,
                            slug,
                            created_at
                        )
                        VALUES (?, ?, ?, ?)
                        ON CONFLICT (id) DO NOTHING
                        """,
                ORGANIZATION_B,
                "Organización de pruebas",
                "pruebas",
                Timestamp.from(Instant.now()));
    }

    @AfterEach
    void limpiarContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void unaOrganizacionNoDebeAccederAlHallazgoDeOtra() {
        autenticarEnOrganizacion(ORGANIZATION_A);

        Finding createdFinding = findingService.create(
                "SQL Injection aislado",
                "Hallazgo perteneciente a la organización A",
                FindingSeverity.HIGH);

        autenticarEnOrganizacion(ORGANIZATION_B);

        assertTrue(
                findingService
                        .findById(createdFinding.id())
                        .isEmpty());

        assertThrows(
                FindingNotFoundException.class,
                () -> findingService.deleteById(createdFinding.id()));

        autenticarEnOrganizacion(ORGANIZATION_A);

        assertTrue(
                findingService
                        .findById(createdFinding.id())
                        .isPresent());
    }

    private void autenticarEnOrganizacion(UUID organizationId) {
        Jwt jwt = Jwt.withTokenValue("token-de-prueba")
                .header("alg", "none")
                .claim("preferred_username", "usuario-prueba")
                .claim("organization_id", organizationId.toString())
                .issuedAt(Instant.now().minusSeconds(60))
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);

        authentication.setAuthenticated(true);

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(authentication);

        SecurityContextHolder.setContext(securityContext);
    }
}