package com.securefindings.comment.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

import com.securefindings.audit.application.AuditService;
import com.securefindings.comment.domain.FindingComment;
import com.securefindings.comment.persistence.FindingCommentEntity;
import com.securefindings.comment.persistence.FindingCommentRepository;
import com.securefindings.finding.application.FindingNotFoundException;
import com.securefindings.finding.domain.Finding;
import com.securefindings.finding.domain.FindingSeverity;
import com.securefindings.finding.persistence.FindingEntity;
import com.securefindings.finding.persistence.FindingRepository;
import com.securefindings.security.OrganizationContext;

@ExtendWith(MockitoExtension.class)
class FindingCommentServiceTest {

    private static final UUID ORGANIZATION_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000001");

    @Mock
    private FindingCommentRepository commentRepository;

    @Mock
    private FindingRepository findingRepository;

    @Mock
    private OrganizationContext organizationContext;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private FindingCommentService commentService;

    @Test
    void deberiaCrearUnComentarioConActorSystem() {
        Finding finding = Finding.create(
                "SQL Injection",
                "Entrada sin validar",
                FindingSeverity.HIGH);

        when(organizationContext.currentOrganizationId())
                .thenReturn(ORGANIZATION_ID);

        when(findingRepository.findByIdAndOrganizationId(
                finding.id(),
                ORGANIZATION_ID))
                .thenReturn(Optional.of(new FindingEntity(finding)));

        when(commentRepository.save(any(FindingCommentEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FindingComment createdComment = commentService.create(
                finding.id(),
                "Se ha revisado el hallazgo.");

        assertEquals(finding.id(), createdComment.findingId());
        assertEquals(ORGANIZATION_ID, createdComment.organizationId());
        assertEquals("system", createdComment.author());
        assertEquals(
                "Se ha revisado el hallazgo.",
                createdComment.content());

        verify(findingRepository).findByIdAndOrganizationId(
                finding.id(),
                ORGANIZATION_ID);

        verify(commentRepository)
                .save(any(FindingCommentEntity.class));
    }

    @Test
    void deberiaDevolverLosComentariosPaginados() {
        Finding finding = Finding.create(
                "Cross-Site Scripting",
                "Contenido sin escapar",
                FindingSeverity.MEDIUM);

        FindingComment comment = new FindingComment(
                UUID.randomUUID(),
                finding.id(),
                ORGANIZATION_ID,
                "analista",
                "Comentario de revisión",
                Instant.parse("2026-09-08T10:00:00Z"));

        when(organizationContext.currentOrganizationId())
                .thenReturn(ORGANIZATION_ID);

        when(findingRepository.findByIdAndOrganizationId(
                finding.id(),
                ORGANIZATION_ID))
                .thenReturn(Optional.of(new FindingEntity(finding)));

        Page<FindingCommentEntity> commentPage = new PageImpl<>(
                List.of(new FindingCommentEntity(comment)),
                PageRequest.of(0, 20),
                1);

        when(commentRepository
                .findByFindingIdAndOrganizationIdOrderByCreatedAtAscIdAsc(
                        eq(finding.id()),
                        eq(ORGANIZATION_ID),
                        any(Pageable.class)))
                .thenReturn(commentPage);

        Page<FindingComment> result = commentService
                .findPageByFindingId(finding.id(), 0, 20);

        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertEquals("analista",
                result.getContent().get(0).author());
        assertEquals(
                "Comentario de revisión",
                result.getContent().get(0).content());

        verify(commentRepository)
                .findByFindingIdAndOrganizationIdOrderByCreatedAtAscIdAsc(
                        eq(finding.id()),
                        eq(ORGANIZATION_ID),
                        any(Pageable.class));
    }

    @Test
    void deberiaRechazarUnHallazgoDeOtraOrganizacion() {
        UUID findingId = UUID.randomUUID();

        when(organizationContext.currentOrganizationId())
                .thenReturn(ORGANIZATION_ID);

        when(findingRepository.findByIdAndOrganizationId(
                findingId,
                ORGANIZATION_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                FindingNotFoundException.class,
                () -> commentService.create(
                        findingId,
                        "Comentario no autorizado"));

        verify(commentRepository, never())
                .save(any(FindingCommentEntity.class));
    }
}