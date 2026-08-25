package com.campusops.incident.service.impl;

import com.campusops.incident.client.AssetServiceClient;
import com.campusops.incident.client.dto.AssetResponseDto;
import com.campusops.incident.dto.request.AssignIncidentRequest;
import com.campusops.incident.dto.request.CreateIncidentRequest;
import com.campusops.incident.dto.request.UpdateIncidentRequest;
import com.campusops.incident.dto.request.UpdateIncidentStatusRequest;
import com.campusops.incident.dto.response.IncidentResponse;
import com.campusops.incident.entity.Incident;
import com.campusops.incident.entity.IncidentStatus;
import com.campusops.incident.event.IncidentAssignedEvent;
import com.campusops.incident.event.IncidentCreatedEvent;
import com.campusops.incident.exception.ForbiddenException;
import com.campusops.incident.exception.ResourceNotFoundException;
import com.campusops.incident.repository.IncidentRepository;
import com.campusops.incident.security.CurrentUser;
import com.campusops.incident.security.UserRole;
import com.campusops.incident.service.IncidentService;
import com.campusops.incident.service.SlaCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncidentServiceImpl implements IncidentService {

    private final IncidentRepository incidentRepository;
    private final AssetServiceClient assetServiceClient;
    private final ApplicationEventPublisher eventPublisher;
    private final SlaCalculator slaCalculator;

    @Override
    @Transactional
    public IncidentResponse createIncident(CreateIncidentRequest request, CurrentUser currentUser) {
        if (currentUser.role() != UserRole.STUDENT) {
            throw new ForbiddenException("Only STUDENT users can report incidents");
        }

        if (request.getAssetId() != null && !request.getAssetId().isBlank()) {
            Optional<AssetResponseDto> asset = assetServiceClient.getAssetById(request.getAssetId());
            if (asset.isEmpty()) {
                throw new IllegalArgumentException("Asset not found with id: " + request.getAssetId());
            }
        }

        // Caller identity always comes from the JWT subject - never from the request body
        Incident incident = Incident.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority())
                .assetId(request.getAssetId())
                .reporterId(currentUser.subject())
                .status(IncidentStatus.OPEN)
                .slaDeadline(slaCalculator.calculateDeadline(request.getPriority()))
                .active(true)
                .build();

        Incident saved = incidentRepository.save(incident);

        eventPublisher.publishEvent(IncidentCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .occurredAt(Instant.now())
                .incidentId(saved.getId())
                .title(saved.getTitle())
                .priority(saved.getPriority())
                .assetId(saved.getAssetId())
                .reporterId(saved.getReporterId())
                .build());

        return IncidentResponse.fromEntity(saved);
    }

    @Override
    public IncidentResponse getIncidentById(Long id, CurrentUser currentUser) {
        Incident incident = incidentRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found with id: " + id));
        authorizeReadAccess(incident, currentUser);
        return IncidentResponse.fromEntity(incident);
    }

    @Override
    public List<IncidentResponse> getAllIncidents(IncidentStatus status, CurrentUser currentUser) {
        List<Incident> incidents;
        switch (currentUser.role()) {
            case MANAGER -> incidents = status != null
                    ? incidentRepository.findByActiveTrueAndStatus(status)
                    : incidentRepository.findByActiveTrue();
            case STUDENT -> incidents = filterByStatus(
                    incidentRepository.findByReporterIdAndActiveTrue(currentUser.subject()), status);
            case TECHNICIAN -> incidents = filterByStatus(
                    incidentRepository.findByAssigneeIdAndActiveTrue(currentUser.subject()), status);
            default -> throw new ForbiddenException("Access denied");
        }
        return incidents.stream()
                .map(IncidentResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public IncidentResponse updateIncident(Long id, UpdateIncidentRequest request, CurrentUser currentUser) {
        Incident incident = incidentRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found with id: " + id));
        authorizeUpdateAccess(incident, currentUser);

        if (request.getAssetId() != null && !request.getAssetId().isBlank()) {
            Optional<AssetResponseDto> asset = assetServiceClient.getAssetById(request.getAssetId());
            if (asset.isEmpty()) {
                throw new IllegalArgumentException("Asset not found with id: " + request.getAssetId());
            }
        }

        boolean priorityChanged = incident.getPriority() != request.getPriority();

        incident.setTitle(request.getTitle());
        incident.setDescription(request.getDescription());
        incident.setPriority(request.getPriority());
        incident.setAssetId(request.getAssetId());

        if (priorityChanged) {
            incident.setSlaDeadline(slaCalculator.calculateDeadline(request.getPriority()));
        }

        Incident updated = incidentRepository.save(incident);
        return IncidentResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public IncidentResponse updateIncidentStatus(Long id, UpdateIncidentStatusRequest request, CurrentUser currentUser) {
        Incident incident = incidentRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found with id: " + id));
        authorizeProgressAccess(incident, currentUser);

        IncidentStatus currentStatus = incident.getStatus();
        IncidentStatus newStatus = request.getStatus();

        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    String.format("Invalid status transition from %s to %s", currentStatus, newStatus)
            );
        }

        incident.setStatus(newStatus);
        Incident updated = incidentRepository.save(incident);
        return IncidentResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public IncidentResponse assignIncident(Long id, AssignIncidentRequest request) {
        Incident incident = incidentRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found with id: " + id));

        if (!incident.getStatus().canTransitionTo(IncidentStatus.ASSIGNED)) {
            throw new IllegalStateException(
                    String.format("Invalid status transition from %s to %s", incident.getStatus(), IncidentStatus.ASSIGNED)
            );
        }

        incident.setAssigneeId(request.getAssigneeId());
        incident.setStatus(IncidentStatus.ASSIGNED);
        Incident updated = incidentRepository.save(incident);

        eventPublisher.publishEvent(IncidentAssignedEvent.builder()
                .eventId(UUID.randomUUID())
                .occurredAt(Instant.now())
                .incidentId(updated.getId())
                .title(updated.getTitle())
                .assigneeId(updated.getAssigneeId())
                .build());

        return IncidentResponse.fromEntity(updated);
    }

    private void authorizeReadAccess(Incident incident, CurrentUser currentUser) {
        switch (currentUser.role()) {
            case MANAGER -> {
            }
            case STUDENT -> requireOwnership(
                    currentUser.subject().equals(incident.getReporterId()),
                    "Students may only view their own incidents");
            case TECHNICIAN -> requireOwnership(
                    currentUser.subject().equals(incident.getAssigneeId()),
                    "Technicians may only view incidents assigned to them");
        }
    }

    private void authorizeUpdateAccess(Incident incident, CurrentUser currentUser) {
        switch (currentUser.role()) {
            case MANAGER -> {
            }
            case STUDENT -> requireOwnership(
                    currentUser.subject().equals(incident.getReporterId()),
                    "Students may only update their own incidents");
            case TECHNICIAN -> throw new ForbiddenException("Technicians may not update incident details");
        }
    }

    private void authorizeProgressAccess(Incident incident, CurrentUser currentUser) {
        switch (currentUser.role()) {
            case MANAGER -> {
            }
            case STUDENT -> requireOwnership(
                    currentUser.subject().equals(incident.getReporterId()),
                    "Students may only progress their own incidents");
            case TECHNICIAN -> requireOwnership(
                    currentUser.subject().equals(incident.getAssigneeId()),
                    "Technicians may only progress incidents assigned to them");
        }
    }

    private void requireOwnership(boolean allowed, String message) {
        if (!allowed) {
            throw new ForbiddenException(message);
        }
    }

    private List<Incident> filterByStatus(List<Incident> incidents, IncidentStatus status) {
        return status == null
                ? incidents
                : incidents.stream().filter(incident -> incident.getStatus() == status).toList();
    }
}
