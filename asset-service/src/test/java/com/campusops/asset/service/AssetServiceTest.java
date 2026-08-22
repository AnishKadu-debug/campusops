package com.campusops.asset.service;

import com.campusops.asset.dto.request.CreateAssetRequest;
import com.campusops.asset.dto.request.UpdateAssetRequest;
import com.campusops.asset.dto.response.AssetResponse;
import com.campusops.asset.entity.Asset;
import com.campusops.asset.entity.AssetStatus;
import com.campusops.asset.entity.AssetType;
import com.campusops.asset.exception.ResourceNotFoundException;
import com.campusops.asset.repository.AssetRepository;
import com.campusops.asset.service.impl.AssetServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @InjectMocks
    private AssetServiceImpl assetService;

    private Asset sampleAsset;

    @BeforeEach
    void setUp() {
        sampleAsset = Asset.builder()
                .id("asset-1")
                .name("Cisco Core Switch 3850")
                .type(AssetType.ROUTER)
                .location("Server Room A")
                .status(AssetStatus.ACTIVE)
                .attributes(Map.of("ip", "192.168.1.1", "ports", 48))
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("createAsset should set default status ACTIVE when not provided and active true")
    void createAsset_shouldSetDefaultStatusAndActive() {
        CreateAssetRequest request = CreateAssetRequest.builder()
                .name("Daikin Split AC 2 Ton")
                .type(AssetType.AC)
                .location("Room 302")
                .attributes(Map.of("capacity", "2 Ton", "refrigerant", "R32"))
                .build();

        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> {
            Asset asset = invocation.getArgument(0);
            asset.setId("asset-2");
            return asset;
        });

        AssetResponse response = assetService.createAsset(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("asset-2");
        assertThat(response.getName()).isEqualTo("Daikin Split AC 2 Ton");
        assertThat(response.getType()).isEqualTo(AssetType.AC);
        assertThat(response.getLocation()).isEqualTo("Room 302");
        assertThat(response.getStatus()).isEqualTo(AssetStatus.ACTIVE);
        assertThat(response.getAttributes()).containsEntry("capacity", "2 Ton");
        assertThat(response.isActive()).isTrue();

        ArgumentCaptor<Asset> captor = ArgumentCaptor.forClass(Asset.class);
        verify(assetRepository).save(captor.capture());
        Asset savedAsset = captor.getValue();
        assertThat(savedAsset.getStatus()).isEqualTo(AssetStatus.ACTIVE);
        assertThat(savedAsset.isActive()).isTrue();
    }

    @Test
    @DisplayName("getAssetById should return active asset when found")
    void getAssetById_shouldReturnActiveAsset() {
        when(assetRepository.findByIdAndActiveTrue("asset-1")).thenReturn(Optional.of(sampleAsset));

        AssetResponse response = assetService.getAssetById("asset-1");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("asset-1");
        assertThat(response.getName()).isEqualTo("Cisco Core Switch 3850");
        assertThat(response.isActive()).isTrue();
    }

    @Test
    @DisplayName("getAssetById should throw ResourceNotFoundException when missing or inactive")
    void getAssetById_shouldThrowWhenNotFoundOrInactive() {
        when(assetRepository.findByIdAndActiveTrue("missing-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assetService.getAssetById("missing-id"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Asset not found with id: missing-id");
    }

    @Test
    @DisplayName("getAllAssets without filters should return all active assets")
    void getAllAssets_withoutFilters_shouldReturnAllActiveAssets() {
        when(assetRepository.findByActiveTrue()).thenReturn(List.of(sampleAsset));

        List<AssetResponse> list = assetService.getAllAssets(null, null);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getId()).isEqualTo("asset-1");
        verify(assetRepository).findByActiveTrue();
    }

    @Test
    @DisplayName("getAllAssets with type filter should return filtered active assets")
    void getAllAssets_withTypeFilter_shouldReturnFilteredAssets() {
        when(assetRepository.findByActiveTrueAndType(AssetType.ROUTER)).thenReturn(List.of(sampleAsset));

        List<AssetResponse> list = assetService.getAllAssets(AssetType.ROUTER, null);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getType()).isEqualTo(AssetType.ROUTER);
        verify(assetRepository).findByActiveTrueAndType(AssetType.ROUTER);
    }

    @Test
    @DisplayName("getAllAssets with status filter should return filtered active assets")
    void getAllAssets_withStatusFilter_shouldReturnFilteredAssets() {
        when(assetRepository.findByActiveTrueAndStatus(AssetStatus.ACTIVE)).thenReturn(List.of(sampleAsset));

        List<AssetResponse> list = assetService.getAllAssets(null, AssetStatus.ACTIVE);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getStatus()).isEqualTo(AssetStatus.ACTIVE);
        verify(assetRepository).findByActiveTrueAndStatus(AssetStatus.ACTIVE);
    }

    @Test
    @DisplayName("getAllAssets with both type and status filters should return filtered active assets")
    void getAllAssets_withTypeAndStatusFilter_shouldReturnFilteredAssets() {
        when(assetRepository.findByActiveTrueAndTypeAndStatus(AssetType.ROUTER, AssetStatus.ACTIVE))
                .thenReturn(List.of(sampleAsset));

        List<AssetResponse> list = assetService.getAllAssets(AssetType.ROUTER, AssetStatus.ACTIVE);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getType()).isEqualTo(AssetType.ROUTER);
        assertThat(list.get(0).getStatus()).isEqualTo(AssetStatus.ACTIVE);
        verify(assetRepository).findByActiveTrueAndTypeAndStatus(AssetType.ROUTER, AssetStatus.ACTIVE);
    }

    @Test
    @DisplayName("updateAsset should update allowed fields")
    void updateAsset_shouldUpdateFields() {
        UpdateAssetRequest updateRequest = UpdateAssetRequest.builder()
                .name("Cisco Core Switch 3850 - Upgraded")
                .type(AssetType.ROUTER)
                .location("Server Room B")
                .status(AssetStatus.UNDER_MAINTENANCE)
                .attributes(Map.of("ip", "192.168.1.2", "firmware", "16.9.4"))
                .build();

        when(assetRepository.findByIdAndActiveTrue("asset-1")).thenReturn(Optional.of(sampleAsset));
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssetResponse response = assetService.updateAsset("asset-1", updateRequest);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Cisco Core Switch 3850 - Upgraded");
        assertThat(response.getLocation()).isEqualTo("Server Room B");
        assertThat(response.getStatus()).isEqualTo(AssetStatus.UNDER_MAINTENANCE);
        assertThat(response.getAttributes()).containsEntry("firmware", "16.9.4");
        assertThat(response.isActive()).isTrue();
    }

    @Test
    @DisplayName("updateAsset should throw ResourceNotFoundException when asset is missing or inactive")
    void updateAsset_shouldThrowWhenNotFound() {
        UpdateAssetRequest updateRequest = UpdateAssetRequest.builder()
                .name("Updated Name")
                .type(AssetType.OTHER)
                .location("Anywhere")
                .build();

        when(assetRepository.findByIdAndActiveTrue("missing-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assetService.updateAsset("missing-id", updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Asset not found with id: missing-id");
    }

    @Test
    @DisplayName("deleteAsset should mark asset as inactive (soft delete)")
    void deleteAsset_shouldMarkInactive() {
        when(assetRepository.findByIdAndActiveTrue("asset-1")).thenReturn(Optional.of(sampleAsset));
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assetService.deleteAsset("asset-1");

        ArgumentCaptor<Asset> captor = ArgumentCaptor.forClass(Asset.class);
        verify(assetRepository).save(captor.capture());
        Asset deletedAsset = captor.getValue();
        assertThat(deletedAsset.isActive()).isFalse();
    }

    @Test
    @DisplayName("deleteAsset should throw ResourceNotFoundException when asset is missing or already inactive")
    void deleteAsset_shouldThrowWhenNotFound() {
        when(assetRepository.findByIdAndActiveTrue("missing-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assetService.deleteAsset("missing-id"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Asset not found with id: missing-id");
    }
}
