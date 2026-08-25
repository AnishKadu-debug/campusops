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
import com.campusops.incident.exception.ResourceNotFoundException;
import com.campusops.incident.repository.IncidentRepository;
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
    public IncidentResponse createIncident(CreateIncidentRequest request) {
        if (request.getAssetId() != null && !request.getAssetId().isBlank()) {
            Optional<AssetResponseDto> asset = assetServiceClient.getAssetById(request.getAssetId());
            if (asset.isEmpty()) {
                throw new IllegalArgumentException("Asset not found with id: " + request.getAssetId());
            }
        }

        Incident incident = Incident.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority())
                .assetId(request.getAssetId())
                .reporterId(request.getReporterId())
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
    public IncidentResponse getIncidentById(Long id) {
        Incident incident = incidentRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found with id: " + id));
        return IncidentResponse.fromEntity(incident);
    }

    @Override
    public List<IncidentResponse> getAllIncidents(IncidentStatus status) {
        List<Incident> incidents;
        if (status != null) {
            incidents = incidentRepository.findByActiveTrueAndStatus(status);
        } else {
            incidents = incidentRepository.findByActiveTrue();
        }
        return incidents.stream()
                .map(IncidentResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public IncidentResponse updateIncident(Long id, UpdateIncidentRequest request) {
        Incident incident = incidentRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found with id: " + id));

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
    public IncidentResponse updateIncidentStatus(Long id, UpdateIncidentStatusRequest request) {
        Incident incident = incidentRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found with id: " + id));

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
}

