package com.campusops.notification.service;

import com.campusops.notification.dto.response.NotificationResponse;

import java.util.List;

public interface NotificationService {

    NotificationResponse recordIncidentCreated(
            com.campusops.notification.event.IncidentCreatedEvent event);

    NotificationResponse recordIncidentAssigned(
            com.campusops.notification.event.IncidentAssignedEvent event);

    NotificationResponse recordSlaBreached(
            com.campusops.notification.event.SlaBreachedEvent event);

    List<NotificationResponse> getNotificationHistory(Long incidentId);
}
