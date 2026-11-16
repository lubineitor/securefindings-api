package com.securefindings.finding.application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.securefindings.asset.domain.AssetStatus;
import com.securefindings.asset.persistence.AssetEntity;
import com.securefindings.asset.persistence.AssetRepository;
import com.securefindings.finding.api.FindingCsvImportResponse;
import com.securefindings.finding.api.FindingCsvParser;
import com.securefindings.finding.api.FindingCsvParser.ParsedRow;
import com.securefindings.finding.domain.Finding;
import com.securefindings.security.OrganizationContext;

@Service
public class FindingCsvImportService {

    private final FindingCsvParser parser;
    private final AssetRepository assetRepository;
    private final OrganizationContext organizationContext;
    private final FindingService findingService;

    public FindingCsvImportService(
            FindingCsvParser parser,
            AssetRepository assetRepository,
            OrganizationContext organizationContext,
            FindingService findingService) {

        this.parser = Objects.requireNonNull(parser);
        this.assetRepository = Objects.requireNonNull(assetRepository);
        this.organizationContext = Objects.requireNonNull(organizationContext);
        this.findingService = Objects.requireNonNull(findingService);
    }

    @Transactional
    public FindingCsvImportResponse importCsv(
            byte[] csvBytes,
            boolean dryRun) {

        FindingCsvParser.ParseResult parseResult = parser.parse(csvBytes);

        if (!parseResult.errors().isEmpty()) {
            return new FindingCsvImportResponse(
                    0,
                    0,
                    dryRun,
                    List.of(),
                    parseResult.errors());
        }

        UUID organizationId = organizationContext.currentOrganizationId();
        List<FindingCsvImportResponse.RowError> errors = new ArrayList<>();
        Map<UUID, Optional<AssetEntity>> assetsById = new HashMap<>();

        for (ParsedRow row : parseResult.rows()) {
            if (row.assetId() == null) {
                continue;
            }

            Optional<AssetEntity> asset = assetsById.computeIfAbsent(
                    row.assetId(),
                    assetId -> assetRepository.findByIdAndOrganizationId(
                            assetId,
                            organizationId));

            if (asset.isEmpty()) {
                errors.add(new FindingCsvImportResponse.RowError(
                        row.rowNumber(),
                        "assetId",
                        "El activo no existe o no pertenece a la organización actual"));
                continue;
            }

            if (asset.get().getStatus() == AssetStatus.ARCHIVED) {
                errors.add(new FindingCsvImportResponse.RowError(
                        row.rowNumber(),
                        "assetId",
                        "No se pueden asociar hallazgos nuevos a un activo archivado"));
            }
        }

        if (!errors.isEmpty()) {
            return new FindingCsvImportResponse(
                    0,
                    0,
                    dryRun,
                    List.of(),
                    errors);
        }

        int validatedCount = parseResult.rows().size();

        if (dryRun) {
            return new FindingCsvImportResponse(
                    0,
                    validatedCount,
                    true,
                    List.of(),
                    List.of());
        }

        List<UUID> findingIds = new ArrayList<>(validatedCount);

        for (ParsedRow row : parseResult.rows()) {
            Finding finding = findingService.create(
                    row.title(),
                    row.description(),
                    row.severity(),
                    row.assetId(),
                    row.dueAt());

            findingIds.add(finding.id());
        }

        return new FindingCsvImportResponse(
                findingIds.size(),
                validatedCount,
                false,
                findingIds,
                List.of());
    }
}