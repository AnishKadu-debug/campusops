package com.campusops.incident.service;

import com.campusops.incident.dto.request.AssignIncidentRequest;
import com.campusops.incident.dto.request.CreateIncidentRequest;
import com.campusops.incident.dto.request.UpdateIncidentRequest;
import com.campusops.incident.dto.request.UpdateIncidentStatusRequest;
import com.campusops.incident.dto.response.IncidentResponse;
import com.campusops.incident.entity.IncidentStatus;

import java.util.List;

public interface IncidentService {

    IncidentResponse createIncident(CreateIncidentRequest request);

    IncidentResponse getIncidentById(Long id);

    List<IncidentResponse> getAllIncidents(IncidentStatus status);

    IncidentResponse updateIncident(Long id, UpdateIncidentRequest request);

    IncidentResponse updateIncidentStatus(Long id, UpdateIncidentStatusRequest request);

    IncidentResponse assignIncident(Long id, AssignIncidentRequest request);
}
