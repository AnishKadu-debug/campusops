package com.campusops.incident.service;

import com.campusops.incident.dto.request.AssignIncidentRequest;
import com.campusops.incident.dto.request.CreateIncidentRequest;
import com.campusops.incident.dto.request.UpdateIncidentRequest;
import com.campusops.incident.dto.request.UpdateIncidentStatusRequest;
import com.campusops.incident.dto.response.IncidentResponse;
import com.campusops.incident.entity.IncidentStatus;
import com.campusops.incident.security.CurrentUser;

import java.util.List;

public interface IncidentService {

    IncidentResponse createIncident(CreateIncidentRequest request, CurrentUser currentUser);

    IncidentResponse getIncidentById(Long id, CurrentUser currentUser);

    List<IncidentResponse> getAllIncidents(IncidentStatus status, CurrentUser currentUser);

    IncidentResponse updateIncident(Long id, UpdateIncidentRequest request, CurrentUser currentUser);

    IncidentResponse updateIncidentStatus(Long id, UpdateIncidentStatusRequest request, CurrentUser currentUser);

    IncidentResponse assignIncident(Long id, AssignIncidentRequest request);
}
