package com.campusops.notification.service;

import com.campusops.notification.dto.response.NotificationResponse;

import java.util.List;

public interface NotificationService {

    NotificationResponse recordIncidentCreated(
            com.campusops.notification.event.IncidentCreatedEvent event);

    NotificationResponse recordIncidentAssigned(
            com.campusops.notification.event.IncidentAssignedEvent event);

    List<NotificationResponse> getNotificationHistory(Long incidentId);
}
