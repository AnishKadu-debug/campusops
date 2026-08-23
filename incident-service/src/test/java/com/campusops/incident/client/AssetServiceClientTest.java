package com.campusops.incident.client;

import com.campusops.incident.client.dto.AssetResponseDto;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetServiceClientTest {

    @Mock
    private AssetClient assetClient;

    @InjectMocks
    private AssetServiceClient assetServiceClient;

    private AssetResponseDto sampleAsset;

    @BeforeEach
    void setUp() {
        sampleAsset = AssetResponseDto.builder()
                .id("ASSET-101")
                .name("Epson Lab Projector")
                .type("PROJECTOR")
                .location("Room 301")
                .status("AVAILABLE")
                .active(true)
                .build();
    }

    @Test
    @DisplayName("getAssetById should return asset response when asset exists in asset-service")
    void getAssetById_whenAssetExists_shouldReturnAsset() {
        when(assetClient.getAssetById("ASSET-101")).thenReturn(sampleAsset);

        Optional<AssetResponseDto> result = assetServiceClient.getAssetById("ASSET-101");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("ASSET-101");
        assertThat(result.get().getName()).isEqualTo("Epson Lab Projector");
        verify(assetClient).getAssetById("ASSET-101");
    }

    @Test
    @DisplayName("getAssetById should return empty Optional when asset-service returns 404 Not Found")
    void getAssetById_whenAssetNotFound_shouldReturnEmpty() {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "/api/v1/assets/UNKNOWN",
                Collections.emptyMap(),
                null,
                new RequestTemplate()
        );
        when(assetClient.getAssetById("UNKNOWN"))
                .thenThrow(new FeignException.NotFound("Asset not found", request, null, Collections.emptyMap()));

        Optional<AssetResponseDto> result = assetServiceClient.getAssetById("UNKNOWN");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getAssetById should return empty Optional when assetId is null or blank")
    void getAssetById_whenAssetIdNullOrBlank_shouldReturnEmpty() {
        assertThat(assetServiceClient.getAssetById(null)).isEmpty();
        assertThat(assetServiceClient.getAssetById("   ")).isEmpty();
    }

    @Test
    @DisplayName("getAssetFallback should provide controlled degraded response preventing cascading failure")
    void getAssetFallback_shouldReturnDegradedFallbackAsset() {
        Throwable cause = new RuntimeException("Asset service connection timed out / unavailable");

        Optional<AssetResponseDto> fallback = assetServiceClient.getAssetFallback("ASSET-101", cause);

        assertThat(fallback).isPresent();
        assertThat(fallback.get().getId()).isEqualTo("ASSET-101");
        assertThat(fallback.get().getName()).contains("Circuit Breaker Fallback");
        assertThat(fallback.get().isActive()).isTrue();
    }
}
