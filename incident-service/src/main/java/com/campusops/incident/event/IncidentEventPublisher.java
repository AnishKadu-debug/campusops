package com.campusops.incident.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class IncidentEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String incidentCreatedTopic;
    private final String incidentAssignedTopic;

    public IncidentEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${campusops.kafka.topics.incident-created}") String incidentCreatedTopic,
            @Value("${campusops.kafka.topics.incident-assigned}") String incidentAssignedTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.incidentCreatedTopic = incidentCreatedTopic;
        this.incidentAssignedTopic = incidentAssignedTopic;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onIncidentCreated(IncidentCreatedEvent event) {
        kafkaTemplate.send(incidentCreatedTopic, event.getIncidentId().toString(), event);
        log.info("Published {} to {} for incident {}", event.getClass().getSimpleName(),
                incidentCreatedTopic, event.getIncidentId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onIncidentAssigned(IncidentAssignedEvent event) {
        kafkaTemplate.send(incidentAssignedTopic, event.getIncidentId().toString(), event);
        log.info("Published {} to {} for incident {}", event.getClass().getSimpleName(),
                incidentAssignedTopic, event.getIncidentId());
    }
}
