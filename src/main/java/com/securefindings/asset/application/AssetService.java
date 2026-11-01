package com.securefindings.asset.application;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.securefindings.asset.domain.Asset;
import com.securefindings.asset.domain.AssetAuditAction;
import com.securefindings.asset.domain.AssetCriticality;
import com.securefindings.asset.domain.AssetEnvironment;
import com.securefindings.asset.domain.AssetStatus;
import com.securefindings.asset.domain.AssetType;
import com.securefindings.asset.persistence.AssetEntity;
import com.securefindings.asset.persistence.AssetRepository;
import com.securefindings.finding.persistence.FindingRepository;
import com.securefindings.security.OrganizationContext;

@Service
@Transactional(readOnly = true)
public class AssetService {

        public static final int MAX_EXPORT_ROWS = 1000;

        private final AssetRepository assetRepository;
        private final FindingRepository findingRepository;
        private final OrganizationContext organizationContext;
        private final AssetAuditService assetAuditService;

        public AssetService(
                        AssetRepository assetRepository,
                        FindingRepository findingRepository,
                        OrganizationContext organizationContext,
                        AssetAuditService assetAuditService) {
                this.assetRepository = Objects.requireNonNull(assetRepository);
                this.findingRepository = Objects.requireNonNull(findingRepository);
                this.organizationContext = Objects.requireNonNull(organizationContext);
                this.assetAuditService = Objects.requireNonNull(assetAuditService);
        }

        public Page<Asset> findPage(
                        int page,
                        int size,
                        String searchTerm,
                        AssetType type,
                        AssetEnvironment environment,
                        AssetCriticality criticality,
                        AssetStatus status) {
                UUID organizationId = organizationContext.currentOrganizationId();
                Pageable pageable = PageRequest.of(
                                page,
                                size,
                                Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id")));

                return assetRepository.findPageByOrganizationAndFilters(
                                organizationId,
                                normalizeSearchTerm(searchTerm),
                                type,
                                environment,
                                criticality,
                                status,
                                pageable)
                                .map(entity -> Objects.requireNonNull(
                                                entity,
                                                "El repositorio devolvió una entidad nula")
                                                .toDomain());
        }

        public List<Asset> findForExport(
                        String searchTerm,
                        AssetType type,
                        AssetEnvironment environment,
                        AssetCriticality criticality,
                        AssetStatus status) {

                UUID organizationId = organizationContext.currentOrganizationId();

                Pageable pageable = PageRequest.of(
                                0,
                                MAX_EXPORT_ROWS + 1,
                                Sort.by(
                                                Sort.Order.asc("name"),
                                                Sort.Order.asc("id")));

                Page<AssetEntity> page = assetRepository.findPageByOrganizationAndFilters(
                                organizationId,
                                normalizeSearchTerm(searchTerm),
                                type,
                                environment,
                                criticality,
                                status,
                                pageable);

                if (page.getTotalElements() > MAX_EXPORT_ROWS
                                || page.getContent().size() > MAX_EXPORT_ROWS) {
                        throw new AssetExportLimitExceededException(MAX_EXPORT_ROWS);
                }

                return page.getContent()
                                .stream()
                                .map(entity -> Objects.requireNonNull(
                                                entity,
                                                "El repositorio devolvió una entidad nula")
                                                .toDomain())
                                .toList();
        }

        public Asset getById(UUID id) {
                UUID organizationId = organizationContext.currentOrganizationId();
                return assetRepository.findByIdAndOrganizationId(id, organizationId)
                                .map(entity -> Objects.requireNonNull(
                                                entity,
                                                "El repositorio devolvió una entidad nula")
                                                .toDomain())
                                .orElseThrow(() -> new AssetNotFoundException(id));
        }

        @Transactional
        public Asset create(
                        String name,
                        String identifier,
                        AssetType type,
                        AssetEnvironment environment,
                        AssetCriticality criticality) {
                UUID organizationId = organizationContext.currentOrganizationId();
                String normalizedIdentifier = normalizeIdentifier(identifier);
                ensureIdentifierIsAvailable(organizationId, normalizedIdentifier, null);

                Asset asset = Asset.create(
                                name,
                                normalizedIdentifier,
                                type,
                                environment,
                                criticality);
                Asset savedAsset = save(asset, organizationId);
                assetAuditService.register(savedAsset.id(), AssetAuditAction.CREATED);
                return savedAsset;
        }

        @Transactional
        public Asset update(
                        UUID id,
                        String name,
                        String identifier,
                        AssetType type,
                        AssetEnvironment environment,
                        AssetCriticality criticality,
                        AssetStatus status) {
                UUID organizationId = organizationContext.currentOrganizationId();
                Asset current = getById(id);
                String updatedName = name == null ? current.name() : name;
                String updatedIdentifier = identifier == null
                                ? current.identifier()
                                : normalizeIdentifier(identifier);
                AssetType updatedType = type == null ? current.type() : type;
                AssetEnvironment updatedEnvironment = environment == null
                                ? current.environment()
                                : environment;
                AssetCriticality updatedCriticality = criticality == null
                                ? current.criticality()
                                : criticality;
                AssetStatus updatedStatus = status == null ? current.status() : status;
                ensureIdentifierIsAvailable(organizationId, updatedIdentifier, id);

                Asset updated = current.withDetails(
                                updatedName,
                                updatedIdentifier,
                                updatedType,
                                updatedEnvironment,
                                updatedCriticality,
                                updatedStatus);

                Asset savedAsset = save(updated, organizationId);
                assetAuditService.register(
                                savedAsset.id(),
                                auditAction(current.status(), savedAsset.status()));

                return savedAsset;
        }

        private AssetAuditAction auditAction(
                        AssetStatus previousStatus,
                        AssetStatus updatedStatus) {

                if (previousStatus == updatedStatus) {
                        return AssetAuditAction.UPDATED;
                }

                return updatedStatus == AssetStatus.ARCHIVED
                                ? AssetAuditAction.ARCHIVED
                                : AssetAuditAction.RESTORED;
        }

        @Transactional
        public void delete(UUID id) {
                UUID organizationId = organizationContext.currentOrganizationId();
                AssetEntity entity = assetRepository.findByIdAndOrganizationId(id, organizationId)
                                .orElseThrow(() -> new AssetNotFoundException(id));

                if (findingRepository.existsByOrganizationIdAndAssetId(organizationId, id)) {
                        throw new AssetHasFindingsException(id);
                }

                assetAuditService.register(id, AssetAuditAction.DELETED);

                try {
                        assetRepository.delete(entity);
                        assetRepository.flush();
                } catch (DataIntegrityViolationException exception) {
                        throw new AssetHasFindingsException(id);
                }
        }

        private Asset save(Asset asset, UUID organizationId) {
                try {
                        return assetRepository.saveAndFlush(
                                        new AssetEntity(asset, organizationId))
                                        .toDomain();
                } catch (DataIntegrityViolationException exception) {
                        throw new AssetIdentifierAlreadyExistsException(
                                        asset.identifier(),
                                        exception);
                }
        }

        private void ensureIdentifierIsAvailable(
                        UUID organizationId,
                        String identifier,
                        UUID assetId) {
                boolean exists = assetId == null
                                ? assetRepository.existsByOrganizationIdAndIdentifierIgnoreCase(
                                                organizationId,
                                                identifier)
                                : assetRepository.existsByOrganizationIdAndIdentifierIgnoreCaseAndIdNot(
                                                organizationId,
                                                identifier,
                                                assetId);

                if (exists) {
                        throw new AssetIdentifierAlreadyExistsException(identifier);
                }
        }

        private String normalizeIdentifier(String identifier) {
                if (identifier == null) {
                        return null;
                }
                return identifier.trim();
        }

        private String normalizeSearchTerm(String searchTerm) {
                if (searchTerm == null || searchTerm.isBlank()) {
                        return null;
                }
                return searchTerm.trim()
                                .replace("!", "!!")
                                .replace("%", "!%")
                                .replace("_", "!_");
        }
}