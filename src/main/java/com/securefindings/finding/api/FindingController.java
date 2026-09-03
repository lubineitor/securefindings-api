package com.securefindings.finding.api;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.securefindings.finding.application.FindingService;
import com.securefindings.finding.domain.Finding;
import com.securefindings.finding.domain.FindingSeverity;
import com.securefindings.finding.domain.FindingStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Validated
@RestController
@RequestMapping("/api/v1/findings")
public class FindingController {

    private final FindingService findingService;

    public FindingController(FindingService findingService) {
        this.findingService = findingService;
    }

    @GetMapping
    public FindingPageResponse findAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,

            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,

            @RequestParam(required = false) FindingSeverity severity,

            @RequestParam(required = false) FindingStatus status) {

        Page<Finding> findingPage = findingService.findPage(
                page,
                size,
                severity,
                status);

        return FindingPageResponse.from(findingPage);
    }

    @GetMapping("/{id}")
    public Finding findById(@PathVariable UUID id) {
        return findingService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Finding create(
            @Valid @RequestBody CreateFindingRequest request) {

        return findingService.create(
                request.title(),
                request.description(),
                request.severity());
    }

    @PatchMapping("/{id}/status")
    public Finding updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFindingStatusRequest request) {

        return findingService.updateStatus(
                id,
                request.status());
    }

    @PutMapping("/{id}")
    public Finding update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFindingRequest request) {

        return findingService.update(
                id,
                request.title(),
                request.description(),
                request.severity());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        findingService.deleteById(id);
    }
}