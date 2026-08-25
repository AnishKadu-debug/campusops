package com.campusops.notification.event;

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
public class IncidentAssignedEvent {

    private UUID eventId;
    private Instant occurredAt;
    private Long incidentId;
    private String title;
    private String assigneeId;
}
