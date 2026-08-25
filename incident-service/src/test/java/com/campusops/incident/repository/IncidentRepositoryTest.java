package com.campusops.incident.repository;

import com.campusops.incident.config.KafkaTestConfig;
import com.campusops.incident.entity.Incident;
import com.campusops.incident.entity.IncidentPriority;
import com.campusops.incident.entity.IncidentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

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
}
