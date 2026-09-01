package com.securefindings.finding.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.securefindings.finding.application.FindingService;
import com.securefindings.finding.domain.Finding;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/findings")
public class FindingController {

    private final FindingService findingService;

    public FindingController(FindingService findingService) {
        this.findingService = findingService;
    }

    @GetMapping
    public List<Finding> findAll() {
        return findingService.findAll();
    }

    @GetMapping("/{id}")
    public Finding findById(@PathVariable UUID id) {
        return findingService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Finding create(@Valid @RequestBody CreateFindingRequest request) {
        return findingService.create(
                request.title(),
                request.description(),
                request.severity());
    }

    @PatchMapping("/{id}/status")
    public Finding updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFindingStatusRequest request) {
        return findingService.updateStatus(id, request.status());
    }
}