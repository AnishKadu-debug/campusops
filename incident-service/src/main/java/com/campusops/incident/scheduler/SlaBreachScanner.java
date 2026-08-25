package com.campusops.incident.scheduler;

import com.campusops.incident.entity.Incident;
import com.campusops.incident.entity.IncidentStatus;
import com.campusops.incident.event.SlaBreachedEvent;
import com.campusops.incident.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlaBreachScanner {

    public static final List<IncidentStatus> BREACH_ELIGIBLE_STATUSES = List.of(
            IncidentStatus.OPEN,
            IncidentStatus.ASSIGNED,
            IncidentStatus.IN_PROGRESS);

    private final IncidentRepository incidentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(fixedDelayString = "${campusops.sla.scan-interval}")
    @Transactional
    public void scanForBreachedIncidents() {
        List<Incident> breachedIncidents = incidentRepository
                .findByActiveTrueAndStatusInAndSlaDeadlineBeforeAndSlaBreachedAtIsNull(
                        BREACH_ELIGIBLE_STATUSES, Instant.now());

        for (Incident incident : breachedIncidents) {
            markBreached(incident);
        }

        if (!breachedIncidents.isEmpty()) {
            log.info("SLA breach scan detected and marked {} incident(s)", breachedIncidents.size());
        }
    }

    private void markBreached(Incident incident) {
        Instant breachedAt = Instant.now();
        incident.setSlaBreachedAt(breachedAt);

        eventPublisher.publishEvent(SlaBreachedEvent.builder()
                .eventId(UUID.randomUUID())
                .occurredAt(breachedAt)
                .incidentId(incident.getId())
                .title(incident.getTitle())
                .priority(incident.getPriority())
                .reporterId(incident.getReporterId())
                .assigneeId(incident.getAssigneeId())
                .slaDeadline(incident.getSlaDeadline())
                .breachedAt(breachedAt)
                .build());

        log.info("SLA breach detected for incident {}", incident.getId());
    }
}
