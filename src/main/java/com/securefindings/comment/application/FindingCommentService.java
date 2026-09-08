package com.securefindings.comment.application;

import java.util.Objects;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.securefindings.audit.application.AuditService;
import com.securefindings.audit.domain.AuditAction;
import com.securefindings.comment.domain.FindingComment;
import com.securefindings.comment.persistence.FindingCommentEntity;
import com.securefindings.comment.persistence.FindingCommentRepository;
import com.securefindings.finding.application.FindingNotFoundException;
import com.securefindings.finding.persistence.FindingRepository;
import com.securefindings.security.OrganizationContext;

@Service
@Transactional(readOnly = true)
public class FindingCommentService {

    private static final String SYSTEM_ACTOR = "system";

    private final FindingCommentRepository commentRepository;
    private final FindingRepository findingRepository;
    private final OrganizationContext organizationContext;
    private final AuditService auditService;

    public FindingCommentService(
            FindingCommentRepository commentRepository,
            FindingRepository findingRepository,
            OrganizationContext organizationContext,
            AuditService auditService) {

        this.commentRepository = Objects.requireNonNull(
                commentRepository);

        this.findingRepository = Objects.requireNonNull(
                findingRepository);

        this.organizationContext = Objects.requireNonNull(
                organizationContext);

        this.auditService = Objects.requireNonNull(
                auditService);
    }

    @Transactional
    public FindingComment create(
            UUID findingId,
            String content) {

        UUID organizationId = organizationContext
                .currentOrganizationId();

        ensureFindingBelongsToOrganization(
                findingId,
                organizationId);

        String actor = currentActor();

        FindingComment comment = FindingComment.create(
                findingId,
                organizationId,
                actor,
                content);

        FindingComment savedComment = commentRepository
                .save(new FindingCommentEntity(comment))
                .toDomain();

        auditService.register(
                findingId,
                AuditAction.COMMENTED,
                actor);

        return savedComment;
    }

    public Page<FindingComment> findPageByFindingId(
            UUID findingId,
            int page,
            int size) {

        UUID organizationId = organizationContext
                .currentOrganizationId();

        ensureFindingBelongsToOrganization(
                findingId,
                organizationId);

        Pageable pageable = PageRequest.of(page, size);

        return commentRepository
                .findByFindingIdAndOrganizationIdOrderByCreatedAtAscIdAsc(
                        findingId,
                        organizationId,
                        pageable)
                .map(this::toDomain);
    }

    private void ensureFindingBelongsToOrganization(
            UUID findingId,
            UUID organizationId) {

        findingRepository
                .findByIdAndOrganizationId(
                        findingId,
                        organizationId)
                .orElseThrow(() -> new FindingNotFoundException(findingId));
    }

    private FindingComment toDomain(
            FindingCommentEntity entity) {

        return Objects.requireNonNull(
                entity,
                "El repositorio devolvió una entidad nula")
                .toDomain();
    }

    private String currentActor() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {
            return SYSTEM_ACTOR;
        }

        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            String preferredUsername = jwtAuthentication
                    .getToken()
                    .getClaimAsString("preferred_username");

            if (preferredUsername != null
                    && !preferredUsername.isBlank()) {
                return preferredUsername;
            }
        }

        String name = authentication.getName();

        return name == null || name.isBlank()
                ? SYSTEM_ACTOR
                : name;
    }
}