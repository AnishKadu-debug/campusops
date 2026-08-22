package com.campusops.asset.service;

import com.campusops.asset.dto.request.CreateAssetRequest;
import com.campusops.asset.dto.request.UpdateAssetRequest;
import com.campusops.asset.dto.response.AssetResponse;
import com.campusops.asset.entity.AssetStatus;
import com.campusops.asset.entity.AssetType;

import java.util.List;

public interface AssetService {

    AssetResponse createAsset(CreateAssetRequest request);

    AssetResponse getAssetById(String id);

    List<AssetResponse> getAllAssets(AssetType type, AssetStatus status);

    AssetResponse updateAsset(String id, UpdateAssetRequest request);

    void deleteAsset(String id);
}
