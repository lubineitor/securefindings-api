package com.securefindings.finding.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.securefindings.finding.application.FindingService;
import com.securefindings.finding.domain.Finding;

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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Finding create(@RequestBody CreateFindingRequest request) {
        return findingService.create(
                request.title(),
                request.description(),
                request.severity()
        );
    }
}