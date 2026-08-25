package com.campusops.incident.event;

import com.campusops.incident.entity.IncidentPriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlaBreachedEvent {

    private UUID eventId;
    private Instant occurredAt;
    private Long incidentId;
    private String title;
    private IncidentPriority priority;
    private String reporterId;
    private String assigneeId;
    private Instant slaDeadline;
    private Instant breachedAt;
}
