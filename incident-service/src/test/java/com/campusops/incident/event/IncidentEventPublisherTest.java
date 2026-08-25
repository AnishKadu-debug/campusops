package com.campusops.incident.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IncidentEventPublisherTest {

    private static final String CREATED_TOPIC = "incident.created.v1";
    private static final String ASSIGNED_TOPIC = "incident.assigned.v1";

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private IncidentEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new IncidentEventPublisher(kafkaTemplate, CREATED_TOPIC, ASSIGNED_TOPIC);
    }

    @Test
    @DisplayName("onIncidentCreated should send event to incident.created.v1 keyed by incident id")
    void onIncidentCreated_shouldSendToCreatedTopicKeyedByIncidentId() {
        IncidentCreatedEvent event = IncidentCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .occurredAt(Instant.now())
                .incidentId(42L)
                .title("Broken AC")
                .build();

        publisher.onIncidentCreated(event);

        verify(kafkaTemplate).send(eq(CREATED_TOPIC), eq("42"), eq(event));
    }

    @Test
    @DisplayName("onIncidentAssigned should send event to incident.assigned.v1 keyed by incident id")
    void onIncidentAssigned_shouldSendToAssignedTopicKeyedByIncidentId() {
        IncidentAssignedEvent event = IncidentAssignedEvent.builder()
                .eventId(UUID.randomUUID())
                .occurredAt(Instant.now())
                .incidentId(42L)
                .assigneeId("tech-9")
                .build();

        publisher.onIncidentAssigned(event);

        verify(kafkaTemplate).send(eq(ASSIGNED_TOPIC), eq("42"), eq(event));
    }

    @Test
    @DisplayName("events should carry a non-null envelope (eventId, occurredAt)")
    void events_shouldCarryEnvelope() {
        Instant now = Instant.now();
        IncidentCreatedEvent created = IncidentCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .occurredAt(now)
                .incidentId(1L)
                .build();

        assertThat(created.getEventId()).isNotNull();
        assertThat(created.getOccurredAt()).isEqualTo(now);
    }
}
