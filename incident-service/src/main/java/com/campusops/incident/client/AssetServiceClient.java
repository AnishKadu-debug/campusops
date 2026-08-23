package com.campusops.incident.client;

import com.campusops.incident.client.dto.AssetResponseDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AssetServiceClient {

    private final AssetClient assetClient;

    @CircuitBreaker(name = "assetService", fallbackMethod = "getAssetFallback")
    public Optional<AssetResponseDto> getAssetById(String assetId) {
        if (assetId == null || assetId.isBlank()) {
            return Optional.empty();
        }
        try {
            AssetResponseDto asset = assetClient.getAssetById(assetId);
            return Optional.ofNullable(asset);
        } catch (feign.FeignException.NotFound e) {
            log.warn("Asset not found in asset-service with id: {}", assetId);
            return Optional.empty();
        }
    }

    public Optional<AssetResponseDto> getAssetFallback(String assetId, Throwable throwable) {
        log.warn("Fallback triggered for assetId: {} due to: {}", assetId, throwable.getMessage());
        AssetResponseDto fallbackAsset = AssetResponseDto.builder()
                .id(assetId)
                .name("UNAVAILABLE (Circuit Breaker Fallback)")
                .active(true)
                .build();
        return Optional.of(fallbackAsset);
    }
}
