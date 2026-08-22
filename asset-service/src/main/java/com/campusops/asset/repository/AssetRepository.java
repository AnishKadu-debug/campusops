package com.campusops.asset.repository;

import com.campusops.asset.entity.Asset;
import com.campusops.asset.entity.AssetStatus;
import com.campusops.asset.entity.AssetType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetRepository extends MongoRepository<Asset, String> {

    Optional<Asset> findByIdAndActiveTrue(String id);

    List<Asset> findByActiveTrue();

    List<Asset> findByActiveTrueAndType(AssetType type);

    List<Asset> findByActiveTrueAndStatus(AssetStatus status);

    List<Asset> findByActiveTrueAndTypeAndStatus(AssetType type, AssetStatus status);
}
