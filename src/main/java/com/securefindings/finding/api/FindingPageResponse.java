package com.securefindings.finding.api;

import java.util.List;

import org.springframework.data.domain.Page;

import com.securefindings.finding.domain.Finding;

public record FindingPageResponse(
        List<Finding> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public static FindingPageResponse from(Page<Finding> findingPage) {
        return new FindingPageResponse(
                findingPage.getContent(),
                findingPage.getNumber(),
                findingPage.getSize(),
                findingPage.getTotalElements(),
                findingPage.getTotalPages(),
                findingPage.isFirst(),
                findingPage.isLast());
    }
}