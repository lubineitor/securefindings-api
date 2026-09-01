package com.securefindings.finding.application;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.securefindings.finding.domain.Finding;
import com.securefindings.finding.domain.FindingSeverity;

@Service
public class FindingService {

    private final Map<UUID, Finding> findings = new ConcurrentHashMap<>();

    public Finding create(
            String title,
            String description,
            FindingSeverity severity) {
        Finding finding = Finding.create(title, description, severity);
        findings.put(finding.id(), finding);
        return finding;
    }

    public List<Finding> findAll() {
        return List.copyOf(findings.values());
    }

    public Optional<Finding> findById(UUID id) {
        return Optional.ofNullable(findings.get(id));
    }

    public Finding getById(UUID id) {
        return findById(id)
                .orElseThrow(() -> new FindingNotFoundException(id));
    }
}