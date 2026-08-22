package com.campusops.asset.dto.response;

import com.campusops.asset.entity.Asset;
import com.campusops.asset.entity.AssetStatus;
import com.campusops.asset.entity.AssetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetResponse {

    private String id;
    private String name;
    private AssetType type;
    private String location;
    private AssetStatus status;
    private Map<String, Object> attributes;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public static AssetResponse fromEntity(Asset asset) {
        if (asset == null) {
            return null;
        }
        return AssetResponse.builder()
                .id(asset.getId())
                .name(asset.getName())
                .type(asset.getType())
                .location(asset.getLocation())
                .status(asset.getStatus())
                .attributes(asset.getAttributes())
                .active(asset.isActive())
                .createdAt(asset.getCreatedAt())
                .updatedAt(asset.getUpdatedAt())
                .build();
    }
}
