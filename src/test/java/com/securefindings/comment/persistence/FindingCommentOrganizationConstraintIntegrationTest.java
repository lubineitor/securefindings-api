package com.securefindings.comment.persistence;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.securefindings.finding.domain.Finding;
import com.securefindings.finding.domain.FindingSeverity;
import com.securefindings.finding.persistence.FindingEntity;
import com.securefindings.finding.persistence.FindingRepository;

@Testcontainers
@SpringBootTest
class FindingCommentOrganizationConstraintIntegrationTest {

    private static final UUID ORGANIZATION_A = UUID.fromString(
            "00000000-0000-0000-0000-000000000001");

    private static final UUID ORGANIZATION_B = UUID.fromString(
            "00000000-0000-0000-0000-000000000002");

    @SuppressWarnings("resource")
    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer(
            "postgres:17-alpine")
            .withDatabaseName("securefindings_test")
            .withUsername("securefindings_test")
            .withPassword("securefindings_test");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FindingRepository findingRepository;

    @DynamicPropertySource
    static void registerPostgresProperties(
            DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    void prepararOrganizacionesYLimpiarDatos() {
        jdbcTemplate.update("DELETE FROM finding_comments");
        findingRepository.deleteAll();

        jdbcTemplate.update("""
                INSERT INTO organizations (id, name, slug, created_at)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (id) DO NOTHING
                """,
                ORGANIZATION_B,
                "Organización B",
                "organizacion-b",
                Timestamp.from(Instant.now()));
    }

    @Test
    void deberiaRechazarUnComentarioDeOtraOrganizacion() {
        Finding finding = Finding.create(
                "SQL Injection",
                "Hallazgo de la organización A",
                FindingSeverity.HIGH);

        findingRepository.saveAndFlush(
                new FindingEntity(finding, ORGANIZATION_A));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> insertarComentario(
                        finding.id(),
                        ORGANIZATION_B));
    }

    @Test
    void deberiaPermitirUnComentarioDeLaMismaOrganizacion() {
        Finding finding = Finding.create(
                "Cross-Site Scripting",
                "Hallazgo de la organización A",
                FindingSeverity.MEDIUM);

        findingRepository.saveAndFlush(
                new FindingEntity(finding, ORGANIZATION_A));

        assertDoesNotThrow(
                () -> insertarComentario(
                        finding.id(),
                        ORGANIZATION_A));
    }

    private void insertarComentario(
            UUID findingId,
            UUID organizationId) {

        jdbcTemplate.update("""
                INSERT INTO finding_comments (
                    id,
                    finding_id,
                    organization_id,
                    author,
                    content,
                    created_at
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                findingId,
                organizationId,
                "analista",
                "Comentario de prueba",
                Timestamp.from(Instant.now()));
    }
}