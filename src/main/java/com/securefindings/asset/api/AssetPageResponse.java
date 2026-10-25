package com.securefindings.asset.api;

import java.util.List;

import org.springframework.data.domain.Page;

import com.securefindings.asset.domain.Asset;

public record AssetPageResponse(
        List<Asset> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public static AssetPageResponse from(Page<Asset> assetPage) {
        return new AssetPageResponse(
                assetPage.getContent(),
                assetPage.getNumber(),
                assetPage.getSize(),
                assetPage.getTotalElements(),
                assetPage.getTotalPages(),
                assetPage.isFirst(),
                assetPage.isLast());
    }
}