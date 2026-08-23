package com.campusops.incident.client;

import com.campusops.incident.client.dto.AssetResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "asset-service", path = "/api/v1/assets")
public interface AssetClient {

    @GetMapping("/{id}")
    AssetResponseDto getAssetById(@PathVariable("id") String id);
}
