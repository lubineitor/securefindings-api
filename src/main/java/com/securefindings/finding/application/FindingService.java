package com.securefindings.finding.application;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.securefindings.audit.application.AuditService;
import com.securefindings.audit.domain.AuditAction;
import com.securefindings.finding.domain.Finding;
import com.securefindings.finding.domain.FindingSeverity;
import com.securefindings.finding.domain.FindingStatus;
import com.securefindings.finding.persistence.FindingEntity;
import com.securefindings.finding.persistence.FindingRepository;

@Service
@Transactional(readOnly = true)
public class FindingService {

    private static final String SYSTEM_ACTOR = "system";

    private final FindingRepository findingRepository;
    private final AuditService auditService;

    public FindingService(
            FindingRepository findingRepository,
            AuditService auditService) {

        this.findingRepository = Objects.requireNonNull(findingRepository);
        this.auditService = Objects.requireNonNull(auditService);
    }

    @Transactional
    public Finding create(
            String title,
            String description,
            FindingSeverity severity) {

        Finding finding = Finding.create(
                title,
                description,
                severity);

        Finding savedFinding = findingRepository
                .save(new FindingEntity(finding))
                .toDomain();

        auditService.register(
                savedFinding.id(),
                AuditAction.CREATED,
                currentActor());

        return savedFinding;
    }

    public List<Finding> findAll() {
        return findingRepository.findAll()
                .stream()
                .map(entity -> Objects.requireNonNull(
                        entity,
                        "El repositorio devolvió una entidad nula").toDomain())
                .toList();
    }

    public Optional<Finding> findById(UUID id) {
        return findingRepository
                .findById(id)
                .map(entity -> Objects.requireNonNull(
                        entity,
                        "El repositorio devolvió una entidad nula").toDomain());
    }

    public Finding getById(UUID id) {
        return findById(id)
                .orElseThrow(() -> new FindingNotFoundException(id));
    }

    @Transactional
    public Finding updateStatus(
            UUID id,
            FindingStatus status) {

        Finding currentFinding = getById(id);
        Finding updatedFinding = currentFinding.withStatus(status);

        Finding savedFinding = save(updatedFinding);

        auditService.register(
                savedFinding.id(),
                AuditAction.UPDATED,
                currentActor());

        return savedFinding;
    }

    @Transactional
    public Finding update(
            UUID id,
            String title,
            String description,
            FindingSeverity severity) {

        Finding currentFinding = getById(id);
        Finding updatedFinding = currentFinding.withDetails(
                title,
                description,
                severity);

        Finding savedFinding = save(updatedFinding);

        auditService.register(
                savedFinding.id(),
                AuditAction.UPDATED,
                currentActor());

        return savedFinding;
    }

    @Transactional
    public void deleteById(UUID id) {
        if (!findingRepository.existsById(id)) {
            throw new FindingNotFoundException(id);
        }

        findingRepository.deleteById(id);

        auditService.register(
                id,
                AuditAction.DELETED,
                currentActor());
    }

    private Finding save(Finding finding) {
        return findingRepository
                .save(new FindingEntity(finding))
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