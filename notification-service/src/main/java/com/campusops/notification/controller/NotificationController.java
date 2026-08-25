package com.campusops.notification.controller;

import com.campusops.notification.dto.response.NotificationResponse;
import com.campusops.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @RequestParam(name = "incidentId", required = false) Long incidentId) {
        List<NotificationResponse> notifications = notificationService.getNotificationHistory(incidentId);
        return ResponseEntity.ok(notifications);
    }
}
