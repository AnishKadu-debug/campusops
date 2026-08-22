package com.campusops.asset.repository;

import com.campusops.asset.entity.Asset;
import com.campusops.asset.entity.AssetStatus;
import com.campusops.asset.entity.AssetType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AssetRepositoryTest {

    @Autowired
    private AssetRepository assetRepository;

    @AfterEach
    void tearDown() {
        assetRepository.deleteAll();
    }

    @Test
    @DisplayName("should save and retrieve active asset and populate audit timestamps")
    void shouldSaveAndRetrieveAsset() {
        Asset asset = Asset.builder()
                .name("Epson Projector")
                .type(AssetType.PROJECTOR)
                .location("Lab 1")
                .status(AssetStatus.ACTIVE)
                .active(true)
                .build();

        Asset saved = assetRepository.save(asset);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        Optional<Asset> found = assetRepository.findByIdAndActiveTrue(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Epson Projector");
        assertThat(found.get().getType()).isEqualTo(AssetType.PROJECTOR);
    }

    @Test
    @DisplayName("findByIdAndActiveTrue should return empty when asset is inactive")
    void findByIdAndActiveTrue_shouldIgnoreInactive() {
        Asset inactiveAsset = Asset.builder()
                .name("Old Router")
                .type(AssetType.ROUTER)
                .location("Server Room")
                .status(AssetStatus.DECOMMISSIONED)
                .active(false)
                .build();

        Asset saved = assetRepository.save(inactiveAsset);

        Optional<Asset> activeFound = assetRepository.findByIdAndActiveTrue(saved.getId());
        assertThat(activeFound).isEmpty();

        // Direct findById still returns the record
        Optional<Asset> directFound = assetRepository.findById(saved.getId());
        assertThat(directFound).isPresent();
        assertThat(directFound.get().isActive()).isFalse();
    }

    @Test
    @DisplayName("findByActiveTrue should only return active assets")
    void findByActiveTrue_shouldFilterCorrectly() {
        Asset activeAsset = Asset.builder()
                .name("Active AC")
                .type(AssetType.AC)
                .location("Room 101")
                .status(AssetStatus.ACTIVE)
                .active(true)
                .build();

        Asset inactiveAsset = Asset.builder()
                .name("Decommissioned AC")
                .type(AssetType.AC)
                .location("Room 102")
                .status(AssetStatus.DECOMMISSIONED)
                .active(false)
                .build();

        assetRepository.save(activeAsset);
        assetRepository.save(inactiveAsset);

        List<Asset> activeList = assetRepository.findByActiveTrue();
        assertThat(activeList).extracting(Asset::isActive).doesNotContain(false);
    }

    @Test
    @DisplayName("findByActiveTrueAndType should filter by type and active flag")
    void findByActiveTrueAndType_shouldFilterByType() {
        Asset projector = Asset.builder()
                .name("Projector X")
                .type(AssetType.PROJECTOR)
                .location("Hall A")
                .status(AssetStatus.ACTIVE)
                .active(true)
                .build();

        Asset router = Asset.builder()
                .name("Router Y")
                .type(AssetType.ROUTER)
                .location("Server Room")
                .status(AssetStatus.ACTIVE)
                .active(true)
                .build();

        assetRepository.save(projector);
        assetRepository.save(router);

        List<Asset> projectors = assetRepository.findByActiveTrueAndType(AssetType.PROJECTOR);
        assertThat(projectors).hasSize(1);
        assertThat(projectors.get(0).getType()).isEqualTo(AssetType.PROJECTOR);
    }
}
