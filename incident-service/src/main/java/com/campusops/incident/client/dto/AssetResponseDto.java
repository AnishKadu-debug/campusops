package com.campusops.incident.client.dto;

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
public class AssetResponseDto {

    private String id;
    private String name;
    private String type;
    private String location;
    private String status;
    private Map<String, Object> attributes;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
