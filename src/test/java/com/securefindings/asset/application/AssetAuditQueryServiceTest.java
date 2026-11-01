package com.securefindings.asset.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.securefindings.asset.domain.AssetAuditAction;
import com.securefindings.asset.persistence.AssetAuditEntity;
import com.securefindings.asset.persistence.AssetAuditRepository;
import com.securefindings.security.OrganizationContext;

@ExtendWith(MockitoExtension.class)
class AssetAuditQueryServiceTest {

    private static final UUID ORGANIZATION_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000001");

    private static final UUID ASSET_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000101");

    private static final Instant OCCURRED_FROM = Instant.parse("2026-10-01T00:00:00Z");

    private static final Instant OCCURRED_TO = Instant.parse("2026-10-31T23:59:59Z");

    @Mock
    private AssetAuditRepository assetAuditRepository;

    @Mock
    private OrganizationContext organizationContext;

    @InjectMocks
    private AssetAuditQueryService assetAuditQueryService;

    @Test
    void deberiaFiltrarPorOrganizacionYAplicarPaginacion() {
        Page<AssetAuditEntity> expectedPage = new PageImpl<>(
                List.of(),
                org.springframework.data.domain.PageRequest.of(2, 10),
                21);

        when(organizationContext.currentOrganizationId()).thenReturn(ORGANIZATION_ID);
        when(assetAuditRepository.findPageByAssetIdAndOrganizationIdAndFilters(
                eq(ASSET_ID),
                eq(ORGANIZATION_ID),
                eq(AssetAuditAction.ARCHIVED),
                eq("asset-request-123"),
                eq("analista"),
                eq(OCCURRED_FROM),
                eq(OCCURRED_TO),
                any(Pageable.class)))
                .thenReturn(expectedPage);

        Page<AssetAuditEntity> result = assetAuditQueryService.findPageByAssetId(
                ASSET_ID,
                2,
                10,
                AssetAuditAction.ARCHIVED,
                "asset-request-123",
                "analista",
                OCCURRED_FROM,
                OCCURRED_TO);

        assertSame(expectedPage, result);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        verify(assetAuditRepository).findPageByAssetIdAndOrganizationIdAndFilters(
                eq(ASSET_ID),
                eq(ORGANIZATION_ID),
                eq(AssetAuditAction.ARCHIVED),
                eq("asset-request-123"),
                eq("analista"),
                eq(OCCURRED_FROM),
                eq(OCCURRED_TO),
                pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertEquals(2, pageable.getPageNumber());
        assertEquals(10, pageable.getPageSize());
        assertEquals(
                Sort.by(
                        Sort.Order.asc("occurredAt"),
                        Sort.Order.asc("id")),
                pageable.getSort());
    }

    @Test
    void deberiaRechazarUnRangoDeFechasInvertido() {
        Instant later = Instant.parse("2026-10-31T00:00:00Z");
        Instant earlier = Instant.parse("2026-10-01T00:00:00Z");

        assertThrows(
                IllegalArgumentException.class,
                () -> assetAuditQueryService.findPageByAssetId(
                        ASSET_ID,
                        0,
                        20,
                        null,
                        null,
                        null,
                        later,
                        earlier));

        verifyNoInteractions(organizationContext, assetAuditRepository);
    }
}