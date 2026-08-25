package com.campusops.notification.consumer;

import com.campusops.notification.event.IncidentAssignedEvent;
import com.campusops.notification.event.IncidentCreatedEvent;
import com.campusops.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IncidentEventConsumerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private IncidentEventConsumer consumer;

    @Test
    @DisplayName("IncidentCreated events should be routed to recordIncidentCreated")
    void onIncidentCreated_shouldDelegateToService() {
        IncidentCreatedEvent event = IncidentCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .occurredAt(Instant.now())
                .incidentId(41L)
                .reporterId("student-123")
                .build();

        consumer.onIncidentCreated(event);

        verify(notificationService).recordIncidentCreated(event);
    }

    @Test
    @DisplayName("IncidentAssigned events should be routed to recordIncidentAssigned")
    void onIncidentAssigned_shouldDelegateToService() {
        IncidentAssignedEvent event = IncidentAssignedEvent.builder()
                .eventId(UUID.randomUUID())
                .occurredAt(Instant.now())
                .incidentId(41L)
                .assigneeId("tech-9")
                .build();

        consumer.onIncidentAssigned(event);

        verify(notificationService).recordIncidentAssigned(event);
    }
}
