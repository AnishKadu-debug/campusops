package com.campusops.incident.repository;

import com.campusops.incident.config.KafkaTestConfig;
import com.campusops.incident.entity.Incident;
import com.campusops.incident.entity.IncidentPriority;
import com.campusops.incident.entity.IncidentStatus;
import com.campusops.incident.scheduler.SlaBreachScanner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(KafkaTestConfig.class)
@Transactional
class IncidentRepositoryTest {

    @Autowired
    private IncidentRepository incidentRepository;

    @Test
    @DisplayName("should save and retrieve active incident and populate audit timestamps")
    void shouldSaveAndRetrieveIncident() {
        Incident incident = Incident.builder()
                .title("Damaged Projector")
                .description("Lamp burned out")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.HIGH)
                .assetId("PROJ-101")
                .reporterId("student-01")
                .active(true)
                .build();

        Incident saved = incidentRepository.save(incident);
        incidentRepository.flush();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        Optional<Incident> found = incidentRepository.findByIdAndActiveTrue(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Damaged Projector");
    }

    @Test
    @DisplayName("findByIdAndActiveTrue should return empty when incident is inactive")
    void findByIdAndActiveTrue_shouldIgnoreInactive() {
        Incident inactiveIncident = Incident.builder()
                .title("Old Incident")
                .description("Legacy issue")
                .status(IncidentStatus.CLOSED)
                .priority(IncidentPriority.LOW)
                .reporterId("student-02")
                .active(false)
                .build();

        Incident saved = incidentRepository.save(inactiveIncident);
        incidentRepository.flush();

        Optional<Incident> activeFound = incidentRepository.findByIdAndActiveTrue(saved.getId());
        assertThat(activeFound).isEmpty();

        // But direct findById still finds the record in DB
        Optional<Incident> directFound = incidentRepository.findById(saved.getId());
        assertThat(directFound).isPresent();
        assertThat(directFound.get().isActive()).isFalse();
    }

    @Test
    @DisplayName("findByActiveTrue should only return active incidents")
    void findByActiveTrue_shouldFilterCorrectly() {
        Incident activeIncident = Incident.builder()
                .title("Active AC Issue")
                .description("AC noisy")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.MEDIUM)
                .reporterId("student-03")
                .active(true)
                .build();

        Incident inactiveIncident = Incident.builder()
                .title("Inactive AC Issue")
                .description("Old noise")
                .status(IncidentStatus.CLOSED)
                .priority(IncidentPriority.LOW)
                .reporterId("student-03")
                .active(false)
                .build();

        incidentRepository.save(activeIncident);
        incidentRepository.save(inactiveIncident);
        incidentRepository.flush();

        List<Incident> activeList = incidentRepository.findByActiveTrue();
        assertThat(activeList).extracting(Incident::isActive).doesNotContain(false);
    }

    @Test
    @DisplayName("breach finder should return only un-breached eligible incidents past their deadline")
    void breachFinder_shouldReturnOnlyEligibleUnbreachedPastDeadlineIncidents() {
        Instant pastDeadline = Instant.now().minusSeconds(60);
        Instant futureDeadline = Instant.now().plusSeconds(3600);

        Incident openBreachedEligible = openIncident("Open Past Deadline", pastDeadline, null);
        Incident assignedBreachedEligible = Incident.builder()
                .title("Assigned Past Deadline")
                .description("Escalated late")
                .status(IncidentStatus.ASSIGNED)
                .priority(IncidentPriority.HIGH)
                .reporterId("student-04")
                .assigneeId("tech-1")
                .slaDeadline(pastDeadline)
                .active(true)
                .build();
        Incident inProgressBreachedEligible = Incident.builder()
                .title("In Progress Past Deadline")
                .description("Still being worked")
                .status(IncidentStatus.IN_PROGRESS)
                .priority(IncidentPriority.MEDIUM)
                .reporterId("student-04")
                .assigneeId("tech-2")
                .slaDeadline(pastDeadline)
                .active(true)
                .build();

        Incident resolvedPastDeadline = Incident.builder()
                .title("Resolved Past Deadline")
                .description("Resolved too late but done")
                .status(IncidentStatus.RESOLVED)
                .priority(IncidentPriority.HIGH)
                .reporterId("student-04")
                .slaDeadline(pastDeadline)
                .active(true)
                .build();

        Incident alreadyMarkedBreached = openIncident("Already Breached", pastDeadline, Instant.now());

        Incident futureDeadlineIncident = openIncident("Future Deadline", futureDeadline, null);

        incidentRepository.saveAll(List.of(
                openBreachedEligible,
                assignedBreachedEligible,
                inProgressBreachedEligible,
                resolvedPastDeadline,
                alreadyMarkedBreached,
                futureDeadlineIncident));
        incidentRepository.flush();

        List<Incident> breached = incidentRepository
                .findByActiveTrueAndStatusInAndSlaDeadlineBeforeAndSlaBreachedAtIsNull(
                        SlaBreachScanner.BREACH_ELIGIBLE_STATUSES, Instant.now());

        assertThat(breached)
                .extracting(Incident::getTitle)
                .containsExactlyInAnyOrder(
                        "Open Past Deadline",
                        "Assigned Past Deadline",
                        "In Progress Past Deadline");
    }

    private Incident openIncident(String title, Instant slaDeadline, Instant slaBreachedAt) {
        return Incident.builder()
                .title(title)
                .description("Breach finder test incident")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.LOW)
                .reporterId("student-04")
                .slaDeadline(slaDeadline)
                .slaBreachedAt(slaBreachedAt)
                .active(true)
                .build();
    }
}
