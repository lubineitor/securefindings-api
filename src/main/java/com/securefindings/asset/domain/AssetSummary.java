package com.securefindings.asset.domain;

import java.util.Map;
import java.util.Objects;

public record AssetSummary(
        long total,
        Map<AssetStatus, Long> byStatus,
        Map<AssetType, Long> byType,
        Map<AssetEnvironment, Long> byEnvironment,
        Map<AssetCriticality, Long> byCriticality) {

    public AssetSummary {
        byStatus = Map.copyOf(Objects.requireNonNull(byStatus));
        byType = Map.copyOf(Objects.requireNonNull(byType));
        byEnvironment = Map.copyOf(Objects.requireNonNull(byEnvironment));
        byCriticality = Map.copyOf(Objects.requireNonNull(byCriticality));
    }
}