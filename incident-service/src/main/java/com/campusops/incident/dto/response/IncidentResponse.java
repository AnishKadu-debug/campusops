package com.campusops.incident.dto.response;

import com.campusops.incident.entity.Incident;
import com.campusops.incident.entity.IncidentPriority;
import com.campusops.incident.entity.IncidentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentResponse {

    private Long id;
    private String title;
    private String description;
    private IncidentStatus status;
    private IncidentPriority priority;
    private String assetId;
    private String reporterId;
    private String assigneeId;
    private Instant slaDeadline;
    private Instant slaBreachedAt;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public static IncidentResponse fromEntity(Incident incident) {
        if (incident == null) {
            return null;
        }
        return IncidentResponse.builder()
                .id(incident.getId())
                .title(incident.getTitle())
                .description(incident.getDescription())
                .status(incident.getStatus())
                .priority(incident.getPriority())
                .assetId(incident.getAssetId())
                .reporterId(incident.getReporterId())
                .assigneeId(incident.getAssigneeId())
                .slaDeadline(incident.getSlaDeadline())
                .slaBreachedAt(incident.getSlaBreachedAt())
                .active(incident.isActive())
                .createdAt(incident.getCreatedAt())
                .updatedAt(incident.getUpdatedAt())
                .build();
    }
}
