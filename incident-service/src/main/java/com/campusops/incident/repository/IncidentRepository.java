package com.campusops.incident.repository;

import com.campusops.incident.entity.Incident;
import com.campusops.incident.entity.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    List<Incident> findByActiveTrue();

    List<Incident> findByActiveTrueAndStatus(IncidentStatus status);

    Optional<Incident> findByIdAndActiveTrue(Long id);

    List<Incident> findByReporterIdAndActiveTrue(String reporterId);

    List<Incident> findByAssigneeIdAndActiveTrue(String assigneeId);

    List<Incident> findByActiveTrueAndStatusInAndSlaDeadlineBeforeAndSlaBreachedAtIsNull(
            Collection<IncidentStatus> statuses, Instant deadline);
}
