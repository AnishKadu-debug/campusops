package com.campusops.asset.service.impl;

import com.campusops.asset.dto.request.CreateAssetRequest;
import com.campusops.asset.dto.request.UpdateAssetRequest;
import com.campusops.asset.dto.response.AssetResponse;
import com.campusops.asset.entity.Asset;
import com.campusops.asset.entity.AssetStatus;
import com.campusops.asset.entity.AssetType;
import com.campusops.asset.exception.ResourceNotFoundException;
import com.campusops.asset.repository.AssetRepository;
import com.campusops.asset.service.AssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService {

    private final AssetRepository assetRepository;

    @Override
    public AssetResponse createAsset(CreateAssetRequest request) {
        Instant now = Instant.now();
        Asset asset = Asset.builder()
                .name(request.getName())
                .type(request.getType())
                .location(request.getLocation())
                .status(request.getStatus() != null ? request.getStatus() : AssetStatus.ACTIVE)
                .attributes(request.getAttributes())
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Asset saved = assetRepository.save(asset);
        return AssetResponse.fromEntity(saved);
    }

    @Override
    public AssetResponse getAssetById(String id) {
        Asset asset = assetRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + id));
        return AssetResponse.fromEntity(asset);
    }

    @Override
    public List<AssetResponse> getAllAssets(AssetType type, AssetStatus status) {
        List<Asset> assets;
        if (type != null && status != null) {
            assets = assetRepository.findByActiveTrueAndTypeAndStatus(type, status);
        } else if (type != null) {
            assets = assetRepository.findByActiveTrueAndType(type);
        } else if (status != null) {
            assets = assetRepository.findByActiveTrueAndStatus(status);
        } else {
            assets = assetRepository.findByActiveTrue();
        }
        return assets.stream()
                .map(AssetResponse::fromEntity)
                .toList();
    }

    @Override
    public AssetResponse updateAsset(String id, UpdateAssetRequest request) {
        Asset asset = assetRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + id));

        asset.setName(request.getName());
        asset.setType(request.getType());
        asset.setLocation(request.getLocation());
        if (request.getStatus() != null) {
            asset.setStatus(request.getStatus());
        }
        asset.setAttributes(request.getAttributes());
        asset.setUpdatedAt(Instant.now());

        Asset updated = assetRepository.save(asset);
        return AssetResponse.fromEntity(updated);
    }

    @Override
    public void deleteAsset(String id) {
        Asset asset = assetRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + id));
        asset.setActive(false);
        asset.setUpdatedAt(Instant.now());
        assetRepository.save(asset);
    }
}
