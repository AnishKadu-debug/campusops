package com.campusops.notification.service.impl;

import com.campusops.notification.dto.response.NotificationResponse;
import com.campusops.notification.entity.Notification;
import com.campusops.notification.entity.NotificationEventType;
import com.campusops.notification.event.IncidentAssignedEvent;
import com.campusops.notification.event.IncidentCreatedEvent;
import com.campusops.notification.repository.NotificationRepository;
import com.campusops.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public NotificationResponse recordIncidentCreated(IncidentCreatedEvent event) {
        Notification notification = Notification.builder()
                .incidentId(event.getIncidentId())
                .eventType(NotificationEventType.INCIDENT_CREATED)
                .recipientId(event.getReporterId())
                .message("Your incident '" + event.getTitle() + "' has been registered.")
                .build();
        return NotificationResponse.fromEntity(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public NotificationResponse recordIncidentAssigned(IncidentAssignedEvent event) {
        Notification notification = Notification.builder()
                .incidentId(event.getIncidentId())
                .eventType(NotificationEventType.INCIDENT_ASSIGNED)
                .recipientId(event.getAssigneeId())
                .message("Incident '" + event.getTitle() + "' has been assigned to you.")
                .build();
        return NotificationResponse.fromEntity(notificationRepository.save(notification));
    }

    @Override
    public List<NotificationResponse> getNotificationHistory(Long incidentId) {
        List<Notification> notifications = incidentId != null
                ? notificationRepository.findByIncidentIdOrderByCreatedAtDesc(incidentId)
                : notificationRepository.findAllByOrderByCreatedAtDesc();
        return notifications.stream()
                .map(NotificationResponse::fromEntity)
                .toList();
    }
}
