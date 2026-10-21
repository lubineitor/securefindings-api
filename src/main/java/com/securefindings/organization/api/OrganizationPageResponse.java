package com.securefindings.organization.api;

import java.util.List;

import org.springframework.data.domain.Page;

import com.securefindings.organization.domain.Organization;

public record OrganizationPageResponse(
        List<OrganizationResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public static OrganizationPageResponse from(Page<Organization> organizationPage) {
        return new OrganizationPageResponse(
                organizationPage.getContent()
                        .stream()
                        .map(OrganizationResponse::from)
                        .toList(),
                organizationPage.getNumber(),
                organizationPage.getSize(),
                organizationPage.getTotalElements(),
                organizationPage.getTotalPages(),
                organizationPage.isFirst(),
                organizationPage.isLast());
    }
}