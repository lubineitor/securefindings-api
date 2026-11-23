package com.securefindings.organization.application;

import java.util.Objects;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.securefindings.organization.persistence.OrganizationStatusAuditEntity;
import com.securefindings.organization.persistence.OrganizationStatusAuditRepository;

@Service
@Transactional(readOnly = true)
public class OrganizationStatusAuditQueryService {

    private final OrganizationStatusAuditRepository auditRepository;

    public OrganizationStatusAuditQueryService(
            OrganizationStatusAuditRepository auditRepository) {
        this.auditRepository = Objects.requireNonNull(auditRepository);
    }

    public Page<OrganizationStatusAuditEntity> findPageByOrganizationId(
            UUID organizationId,
            int page,
            int size) {

        UUID checkedOrganizationId = Objects.requireNonNull(organizationId);

        return auditRepository.findByOrganizationId(
                checkedOrganizationId,
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Order.desc("occurredAt"),
                                Sort.Order.desc("id"))));
    }
}