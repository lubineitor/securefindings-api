package com.securefindings.comment.api;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.securefindings.comment.application.FindingCommentService;
import com.securefindings.comment.domain.FindingComment;
import com.securefindings.security.SecurityConfig;

@WebMvcTest(controllers = FindingCommentController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class
})
@WithMockUser(username = "analista", roles = "ANALYST")
class FindingCommentControllerTest {

    private static final UUID ORGANIZATION_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000001");

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private FindingCommentService commentService;

    private MockMvc mockMvc;

    @BeforeEach
    void configurarMockMvc() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void deberiaCrearUnComentario()
            throws Exception {

        UUID findingId = UUID.randomUUID();

        FindingComment comment = new FindingComment(
                UUID.randomUUID(),
                findingId,
                ORGANIZATION_ID,
                "analista",
                "Se ha revisado el hallazgo.",
                Instant.parse("2026-09-08T10:00:00Z"));

        when(commentService.create(
                findingId,
                "Se ha revisado el hallazgo."))
                .thenReturn(comment);

        mockMvc.perform(post(
                "/api/v1/findings/{findingId}/comments",
                findingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "content": "Se ha revisado el hallazgo."
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id")
                        .value(comment.id().toString()))
                .andExpect(jsonPath("$.findingId")
                        .value(findingId.toString()))
                .andExpect(jsonPath("$.author")
                        .value("analista"))
                .andExpect(jsonPath("$.content")
                        .value("Se ha revisado el hallazgo."));
    }

    @Test
    void deberiaDevolverLosComentariosPaginados()
            throws Exception {

        UUID findingId = UUID.randomUUID();

        FindingComment comment = new FindingComment(
                UUID.randomUUID(),
                findingId,
                ORGANIZATION_ID,
                "analista",
                "Comentario de revisión.",
                Instant.parse("2026-09-08T10:00:00Z"));

        when(commentService.findPageByFindingId(
                findingId,
                0,
                20))
                .thenReturn(new PageImpl<>(
                        List.of(comment),
                        PageRequest.of(0, 20),
                        1));

        mockMvc.perform(get(
                "/api/v1/findings/{findingId}/comments",
                findingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].findingId")
                        .value(findingId.toString()))
                .andExpect(jsonPath("$.content[0].author")
                        .value("analista"))
                .andExpect(jsonPath("$.content[0].content")
                        .value("Comentario de revisión."))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void deberiaRechazarUnComentarioVacio()
            throws Exception {

        UUID findingId = UUID.randomUUID();

        mockMvc.perform(post(
                "/api/v1/findings/{findingId}/comments",
                findingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "content": " "
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.content")
                        .value("El contenido es obligatorio"));
    }

    @Test
    void deberiaRechazarUnaPaginaNegativa()
            throws Exception {

        UUID findingId = UUID.randomUUID();

        mockMvc.perform(get(
                "/api/v1/findings/{findingId}/comments",
                findingId)
                .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"));
    }
}