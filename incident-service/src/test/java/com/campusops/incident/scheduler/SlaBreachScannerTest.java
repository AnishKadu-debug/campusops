package com.campusops.incident.scheduler;

import com.campusops.incident.entity.Incident;
import com.campusops.incident.entity.IncidentPriority;
import com.campusops.incident.entity.IncidentStatus;
import com.campusops.incident.event.SlaBreachedEvent;
import com.campusops.incident.repository.IncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlaBreachScannerTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SlaBreachScanner scanner;

    private Incident breachedIncident;

    @BeforeEach
    void setUp() {
        breachedIncident = Incident.builder()
                .id(7L)
                .title("Broken projector")
                .description("Projector dead in Lab 3")
                .status(IncidentStatus.IN_PROGRESS)
                .priority(IncidentPriority.CRITICAL)
                .reporterId("student-123")
                .assigneeId("tech-9")
                .slaDeadline(Instant.now().minusSeconds(60))
                .active(true)
                .build();
    }

    @Test
    @DisplayName("scan should mark breached incidents and publish one SlaBreachedEvent each")
    void scan_shouldMarkAndPublishEventsForBreachedIncidents() {
        when(incidentRepository.findByActiveTrueAndStatusInAndSlaDeadlineBeforeAndSlaBreachedAtIsNull(
                any(), any())).thenReturn(List.of(breachedIncident));

        scanner.scanForBreachedIncidents();

        assertThat(breachedIncident.getSlaBreachedAt()).isNotNull();

        ArgumentCaptor<SlaBreachedEvent> captor = ArgumentCaptor.forClass(SlaBreachedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        SlaBreachedEvent event = captor.getValue();
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getOccurredAt()).isNotNull();
        assertThat(event.getIncidentId()).isEqualTo(7L);
        assertThat(event.getTitle()).isEqualTo("Broken projector");
        assertThat(event.getPriority()).isEqualTo(IncidentPriority.CRITICAL);
        assertThat(event.getReporterId()).isEqualTo("student-123");
        assertThat(event.getAssigneeId()).isEqualTo("tech-9");
        assertThat(event.getSlaDeadline()).isEqualTo(breachedIncident.getSlaDeadline());
        assertThat(event.getBreachedAt()).isEqualTo(breachedIncident.getSlaBreachedAt());
    }

    @Test
    @DisplayName("scan should publish nothing when no incidents are breached")
    void scan_shouldPublishNothingWhenNoBreaches() {
        when(incidentRepository.findByActiveTrueAndStatusInAndSlaDeadlineBeforeAndSlaBreachedAtIsNull(
                any(), any())).thenReturn(List.of());

        scanner.scanForBreachedIncidents();

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("scan should query only active OPEN/ASSIGNED/IN_PROGRESS incidents past their deadline")
    void scan_shouldQueryOnlyEligibleStatuses() {
        when(incidentRepository.findByActiveTrueAndStatusInAndSlaDeadlineBeforeAndSlaBreachedAtIsNull(
                any(), any())).thenReturn(List.of());

        Instant before = Instant.now();
        scanner.scanForBreachedIncidents();
        Instant after = Instant.now();

        verify(incidentRepository).findByActiveTrueAndStatusInAndSlaDeadlineBeforeAndSlaBreachedAtIsNull(
                argThat(statuses -> statuses.containsAll(SlaBreachScanner.BREACH_ELIGIBLE_STATUSES)),
                argThat(deadline -> !deadline.isBefore(before) && !deadline.isAfter(after)));
    }
}
