package com.campusops.notification.service;

import com.campusops.notification.dto.response.NotificationResponse;
import com.campusops.notification.entity.Notification;
import com.campusops.notification.entity.NotificationEventType;
import com.campusops.notification.event.IncidentAssignedEvent;
import com.campusops.notification.event.IncidentCreatedEvent;
import com.campusops.notification.event.SlaBreachedEvent;
import com.campusops.notification.repository.NotificationRepository;
import com.campusops.notification.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private Instant occurredAt;

    @BeforeEach
    void setUp() {
        occurredAt = Instant.now();
    }

    @Test
    @DisplayName("recordIncidentCreated should map event to INCIDENT_CREATED notification for the reporter")
    void recordIncidentCreated_shouldMapEventToNotification() {
        IncidentCreatedEvent event = IncidentCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .occurredAt(occurredAt)
                .incidentId(41L)
                .title("Projector not working")
                .priority("HIGH")
                .reporterId("student-123")
                .build();

        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = notificationService.recordIncidentCreated(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getIncidentId()).isEqualTo(41L);
        assertThat(saved.getEventType()).isEqualTo(NotificationEventType.INCIDENT_CREATED);
        assertThat(saved.getRecipientId()).isEqualTo("student-123");
        assertThat(saved.getMessage()).contains("Projector not working");

        assertThat(response.getEventType()).isEqualTo(NotificationEventType.INCIDENT_CREATED);
        assertThat(response.getRecipientId()).isEqualTo("student-123");
    }

    @Test
    @DisplayName("recordIncidentAssigned should map event to INCIDENT_ASSIGNED notification for the assignee")
    void recordIncidentAssigned_shouldMapEventToNotification() {
        IncidentAssignedEvent event = IncidentAssignedEvent.builder()
                .eventId(UUID.randomUUID())
                .occurredAt(occurredAt)
                .incidentId(41L)
                .title("Projector not working")
                .assigneeId("tech-9")
                .build();

        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = notificationService.recordIncidentAssigned(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getIncidentId()).isEqualTo(41L);
        assertThat(saved.getEventType()).isEqualTo(NotificationEventType.INCIDENT_ASSIGNED);
        assertThat(saved.getRecipientId()).isEqualTo("tech-9");
        assertThat(saved.getMessage()).contains("assigned to you");

        assertThat(response.getRecipientId()).isEqualTo("tech-9");
    }

    @Test
    @DisplayName("recordSlaBreached should map event to SLA_BREACHED notification for the reporter")
    void recordSlaBreached_shouldMapEventToNotification() {
        SlaBreachedEvent event = SlaBreachedEvent.builder()
                .eventId(UUID.randomUUID())
                .occurredAt(occurredAt)
                .incidentId(41L)
                .title("Projector not working")
                .priority("HIGH")
                .reporterId("student-123")
                .assigneeId("tech-9")
                .slaDeadline(occurredAt.minusSeconds(60))
                .breachedAt(occurredAt)
                .build();

        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = notificationService.recordSlaBreached(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getIncidentId()).isEqualTo(41L);
        assertThat(saved.getEventType()).isEqualTo(NotificationEventType.SLA_BREACHED);
        assertThat(saved.getRecipientId()).isEqualTo("student-123");
        assertThat(saved.getMessage()).contains("Projector not working");

        assertThat(response.getEventType()).isEqualTo(NotificationEventType.SLA_BREACHED);
        assertThat(response.getRecipientId()).isEqualTo("student-123");
    }

    @Test
    @DisplayName("getNotificationHistory should filter by incident when incidentId is given")
    void getNotificationHistory_withIncidentId_shouldFilter() {
        Notification notification = Notification.builder()
                .id(1L)
                .incidentId(41L)
                .eventType(NotificationEventType.INCIDENT_CREATED)
                .recipientId("student-123")
                .message("Your incident 'Projector not working' has been registered.")
                .build();

        when(notificationRepository.findByIncidentIdOrderByCreatedAtDesc(41L))
                .thenReturn(List.of(notification));

        List<NotificationResponse> result = notificationService.getNotificationHistory(41L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIncidentId()).isEqualTo(41L);
        verify(notificationRepository).findByIncidentIdOrderByCreatedAtDesc(41L);
    }

    @Test
    @DisplayName("getNotificationHistory without incidentId should return all notifications")
    void getNotificationHistory_withoutIncidentId_shouldReturnAll() {
        when(notificationRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        List<NotificationResponse> result = notificationService.getNotificationHistory(null);

        assertThat(result).isEmpty();
        verify(notificationRepository).findAllByOrderByCreatedAtDesc();
    }
}
