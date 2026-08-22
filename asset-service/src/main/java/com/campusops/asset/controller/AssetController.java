package com.campusops.asset.controller;

import com.campusops.asset.dto.request.CreateAssetRequest;
import com.campusops.asset.dto.request.UpdateAssetRequest;
import com.campusops.asset.dto.response.AssetResponse;
import com.campusops.asset.entity.AssetStatus;
import com.campusops.asset.entity.AssetType;
import com.campusops.asset.service.AssetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @PostMapping
    public ResponseEntity<AssetResponse> createAsset(@Valid @RequestBody CreateAssetRequest request) {
        AssetResponse response = assetService.createAsset(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<AssetResponse>> getAllAssets(
            @RequestParam(name = "type", required = false) AssetType type,
            @RequestParam(name = "status", required = false) AssetStatus status) {
        List<AssetResponse> assets = assetService.getAllAssets(type, status);
        return ResponseEntity.ok(assets);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssetResponse> getAssetById(@PathVariable("id") String id) {
        AssetResponse response = assetService.getAssetById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AssetResponse> updateAsset(
            @PathVariable("id") String id,
            @Valid @RequestBody UpdateAssetRequest request) {
        AssetResponse response = assetService.updateAsset(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAsset(@PathVariable("id") String id) {
        assetService.deleteAsset(id);
        return ResponseEntity.noContent().build();
    }
}
