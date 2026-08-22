package com.campusops.asset.entity;

import com.campusops.asset.dto.response.AssetResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AssetDomainTest {

    @Test
    @DisplayName("Asset entity and AssetResponse should map all fields accurately including dynamic attributes")
    void assetEntity_andResponseMapping() {
        Instant now = Instant.now();
        Map<String, Object> attributes = Map.of(
                "resolution", "1920x1080",
                "lampHours", 120,
                "hdmiPorts", 2
        );

        Asset asset = Asset.builder()
                .id("asset-101")
                .name("Epson EB-2250U Projector")
                .type(AssetType.PROJECTOR)
                .location("Auditorium B")
                .status(AssetStatus.ACTIVE)
                .attributes(attributes)
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        AssetResponse response = AssetResponse.fromEntity(asset);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("asset-101");
        assertThat(response.getName()).isEqualTo("Epson EB-2250U Projector");
        assertThat(response.getType()).isEqualTo(AssetType.PROJECTOR);
        assertThat(response.getLocation()).isEqualTo("Auditorium B");
        assertThat(response.getStatus()).isEqualTo(AssetStatus.ACTIVE);
        assertThat(response.getAttributes()).containsEntry("resolution", "1920x1080");
        assertThat(response.getAttributes()).containsEntry("lampHours", 120);
        assertThat(response.isActive()).isTrue();
        assertThat(response.getCreatedAt()).isEqualTo(now);
        assertThat(response.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("AssetResponse from null entity should return null")
    void assetResponse_fromNullEntity_shouldReturnNull() {
        assertThat(AssetResponse.fromEntity(null)).isNull();
    }

    @Test
    @DisplayName("Enums should contain expected values per specifications")
    void enumValues_shouldMatchSpecifications() {
        assertThat(AssetType.values()).containsExactly(
                AssetType.PROJECTOR,
                AssetType.ROUTER,
                AssetType.AC,
                AssetType.LAB_EQUIPMENT,
                AssetType.OTHER
        );

        assertThat(AssetStatus.values()).containsExactly(
                AssetStatus.ACTIVE,
                AssetStatus.UNDER_MAINTENANCE,
                AssetStatus.INACTIVE,
                AssetStatus.DECOMMISSIONED
        );
    }
}
