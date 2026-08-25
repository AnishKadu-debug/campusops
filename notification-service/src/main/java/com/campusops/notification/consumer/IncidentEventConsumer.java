package com.campusops.notification.consumer;

import com.campusops.notification.event.IncidentAssignedEvent;
import com.campusops.notification.event.IncidentCreatedEvent;
import com.campusops.notification.event.SlaBreachedEvent;
import com.campusops.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IncidentEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "${campusops.kafka.topics.incident-created}",
            properties = "spring.json.value.default.type=com.campusops.notification.event.IncidentCreatedEvent")
    public void onIncidentCreated(IncidentCreatedEvent event) {
        log.info("Received IncidentCreated event for incident {}", event.getIncidentId());
        notificationService.recordIncidentCreated(event);
    }

    @KafkaListener(
            topics = "${campusops.kafka.topics.incident-assigned}",
            properties = "spring.json.value.default.type=com.campusops.notification.event.IncidentAssignedEvent")
    public void onIncidentAssigned(IncidentAssignedEvent event) {
        log.info("Received IncidentAssigned event for incident {}", event.getIncidentId());
        notificationService.recordIncidentAssigned(event);
    }

    @KafkaListener(
            topics = "${campusops.kafka.topics.sla-breached}",
            properties = "spring.json.value.default.type=com.campusops.notification.event.SlaBreachedEvent")
    public void onSlaBreached(SlaBreachedEvent event) {
        log.info("Received SlaBreached event for incident {}", event.getIncidentId());
        notificationService.recordSlaBreached(event);
    }
}
