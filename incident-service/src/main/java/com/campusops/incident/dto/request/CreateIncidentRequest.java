package com.campusops.incident.dto.request;

import com.campusops.incident.entity.IncidentPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateIncidentRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must not exceed 150 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotNull(message = "Priority is required")
    private IncidentPriority priority;

    @Size(max = 100, message = "Asset ID must not exceed 100 characters")
    private String assetId;

    @NotBlank(message = "Reporter ID is required")
    @Size(max = 100, message = "Reporter ID must not exceed 100 characters")
    private String reporterId;
}
