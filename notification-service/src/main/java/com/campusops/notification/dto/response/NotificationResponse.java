package com.campusops.notification.dto.response;

import com.campusops.notification.entity.Notification;
import com.campusops.notification.entity.NotificationEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private Long incidentId;
    private NotificationEventType eventType;
    private String recipientId;
    private String message;
    private Instant createdAt;
    private Instant updatedAt;

    public static NotificationResponse fromEntity(Notification notification) {
        if (notification == null) {
            return null;
        }
        return NotificationResponse.builder()
                .id(notification.getId())
                .incidentId(notification.getIncidentId())
                .eventType(notification.getEventType())
                .recipientId(notification.getRecipientId())
                .message(notification.getMessage())
                .createdAt(notification.getCreatedAt())
                .updatedAt(notification.getUpdatedAt())
                .build();
    }
}
