package com.campusops.incident.controller;

import com.campusops.incident.dto.request.AssignIncidentRequest;
import com.campusops.incident.dto.request.CreateIncidentRequest;
import com.campusops.incident.dto.request.UpdateIncidentRequest;
import com.campusops.incident.dto.request.UpdateIncidentStatusRequest;
import com.campusops.incident.dto.response.IncidentResponse;
import com.campusops.incident.entity.IncidentStatus;
import com.campusops.incident.security.CurrentUser;
import com.campusops.incident.security.CurrentUserResolver;
import com.campusops.incident.service.IncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;
    private final CurrentUserResolver currentUserResolver;

    @PostMapping
    public ResponseEntity<IncidentResponse> createIncident(
            @Valid @RequestBody CreateIncidentRequest request,
            Authentication authentication) {
        CurrentUser currentUser = currentUserResolver.resolve(authentication);
        IncidentResponse response = incidentService.createIncident(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<IncidentResponse>> getAllIncidents(
            @RequestParam(name = "status", required = false) IncidentStatus status,
            Authentication authentication) {
        CurrentUser currentUser = currentUserResolver.resolve(authentication);
        List<IncidentResponse> incidents = incidentService.getAllIncidents(status, currentUser);
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentResponse> getIncidentById(
            @PathVariable("id") Long id,
            Authentication authentication) {
        CurrentUser currentUser = currentUserResolver.resolve(authentication);
        IncidentResponse response = incidentService.getIncidentById(id, currentUser);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<IncidentResponse> updateIncident(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateIncidentRequest request,
            Authentication authentication) {
        CurrentUser currentUser = currentUserResolver.resolve(authentication);
        IncidentResponse response = incidentService.updateIncident(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<IncidentResponse> updateIncidentStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateIncidentStatusRequest request,
            Authentication authentication) {
        CurrentUser currentUser = currentUserResolver.resolve(authentication);
        IncidentResponse response = incidentService.updateIncidentStatus(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PatchMapping("/{id}/assign")
    public ResponseEntity<IncidentResponse> assignIncident(
            @PathVariable("id") Long id,
            @Valid @RequestBody AssignIncidentRequest request) {
        IncidentResponse response = incidentService.assignIncident(id, request);
        return ResponseEntity.ok(response);
    }
}
