package com.securefindings.finding.application;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.securefindings.finding.domain.Finding;
import com.securefindings.finding.domain.FindingSeverity;
import com.securefindings.finding.domain.FindingStatus;
import com.securefindings.finding.persistence.FindingEntity;
import com.securefindings.finding.persistence.FindingRepository;

@Service
@Transactional(readOnly = true)
public class FindingService {

    private final FindingRepository findingRepository;

    public FindingService(FindingRepository findingRepository) {
        this.findingRepository = Objects.requireNonNull(findingRepository);
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

        FindingEntity entity = new FindingEntity(finding);

        return findingRepository
                .save(entity)
                .toDomain();
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

        return save(updatedFinding);
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

        return save(updatedFinding);
    }

    @Transactional
    public void deleteById(UUID id) {
        if (!findingRepository.existsById(id)) {
            throw new FindingNotFoundException(id);
        }

        findingRepository.deleteById(id);
    }

    private Finding save(Finding finding) {
        FindingEntity entity = new FindingEntity(finding);

        return findingRepository
                .save(entity)
                .toDomain();
    }
}