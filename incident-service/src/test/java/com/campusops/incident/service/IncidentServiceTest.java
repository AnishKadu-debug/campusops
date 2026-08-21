package com.campusops.incident.service;

import com.campusops.incident.dto.request.CreateIncidentRequest;
import com.campusops.incident.dto.request.UpdateIncidentRequest;
import com.campusops.incident.dto.response.IncidentResponse;
import com.campusops.incident.entity.Incident;
import com.campusops.incident.entity.IncidentPriority;
import com.campusops.incident.entity.IncidentStatus;
import com.campusops.incident.exception.ResourceNotFoundException;
import com.campusops.incident.repository.IncidentRepository;
import com.campusops.incident.service.impl.IncidentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @InjectMocks
    private IncidentServiceImpl incidentService;

    private Incident sampleIncident;

    @BeforeEach
    void setUp() {
        sampleIncident = Incident.builder()
                .id(1L)
                .title("Projector not working")
                .description("Projector in Lab 3 is flickering")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.HIGH)
                .assetId("PROJ-LAB-3")
                .reporterId("student-123")
                .active(true)
                .build();
        sampleIncident.setCreatedAt(Instant.now());
        sampleIncident.setUpdatedAt(Instant.now());
    }

    @Test
    @DisplayName("createIncident should set default status OPEN and active true")
    void createIncident_shouldSetDefaultStatusAndActive() {
        CreateIncidentRequest request = CreateIncidentRequest.builder()
                .title("Broken AC")
                .description("AC unit leaking water in Room 204")
                .priority(IncidentPriority.MEDIUM)
                .assetId("AC-204")
                .reporterId("faculty-456")
                .build();

        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> {
            Incident incident = invocation.getArgument(0);
            incident.setId(2L);
            incident.setCreatedAt(Instant.now());
            incident.setUpdatedAt(Instant.now());
            return incident;
        });

        IncidentResponse response = incidentService.createIncident(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.getTitle()).isEqualTo("Broken AC");
        assertThat(response.getDescription()).isEqualTo("AC unit leaking water in Room 204");
        assertThat(response.getStatus()).isEqualTo(IncidentStatus.OPEN);
        assertThat(response.getPriority()).isEqualTo(IncidentPriority.MEDIUM);
        assertThat(response.getAssetId()).isEqualTo("AC-204");
        assertThat(response.getReporterId()).isEqualTo("faculty-456");
        assertThat(response.isActive()).isTrue();

        ArgumentCaptor<Incident> captor = ArgumentCaptor.forClass(Incident.class);
        verify(incidentRepository).save(captor.capture());
        Incident savedIncident = captor.getValue();
        assertThat(savedIncident.getStatus()).isEqualTo(IncidentStatus.OPEN);
        assertThat(savedIncident.isActive()).isTrue();
    }

    @Test
    @DisplayName("getIncidentById should return active incident when found")
    void getIncidentById_shouldReturnActiveIncident() {
        when(incidentRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(sampleIncident));

        IncidentResponse response = incidentService.getIncidentById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Projector not working");
        assertThat(response.getStatus()).isEqualTo(IncidentStatus.OPEN);
        assertThat(response.isActive()).isTrue();
    }

    @Test
    @DisplayName("getIncidentById should throw ResourceNotFoundException when missing or inactive")
    void getIncidentById_shouldThrowWhenNotFoundOrInactive() {
        when(incidentRepository.findByIdAndActiveTrue(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incidentService.getIncidentById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Incident not found with id: 999");
    }

    @Test
    @DisplayName("getAllIncidents without filter should return all active incidents")
    void getAllIncidents_withoutFilter_shouldReturnActiveIncidents() {
        when(incidentRepository.findByActiveTrue()).thenReturn(List.of(sampleIncident));

        List<IncidentResponse> list = incidentService.getAllIncidents(null);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getId()).isEqualTo(1L);
        verify(incidentRepository).findByActiveTrue();
    }

    @Test
    @DisplayName("getAllIncidents with status filter should return filtered active incidents")
    void getAllIncidents_withStatusFilter_shouldReturnFilteredActiveIncidents() {
        when(incidentRepository.findByActiveTrueAndStatus(IncidentStatus.OPEN))
                .thenReturn(List.of(sampleIncident));

        List<IncidentResponse> list = incidentService.getAllIncidents(IncidentStatus.OPEN);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getStatus()).isEqualTo(IncidentStatus.OPEN);
        verify(incidentRepository).findByActiveTrueAndStatus(IncidentStatus.OPEN);
    }

    @Test
    @DisplayName("updateIncident should update allowed fields and preserve status and active flag")
    void updateIncident_shouldUpdateAllowedFieldsAndPreserveStatus() {
        UpdateIncidentRequest updateRequest = UpdateIncidentRequest.builder()
                .title("Updated: Projector completely dead")
                .description("Lamp burned out completely")
                .priority(IncidentPriority.CRITICAL)
                .assetId("PROJ-LAB-3-V2")
                .build();

        when(incidentRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(sampleIncident));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IncidentResponse response = incidentService.updateIncident(1L, updateRequest);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Updated: Projector completely dead");
        assertThat(response.getDescription()).isEqualTo("Lamp burned out completely");
        assertThat(response.getPriority()).isEqualTo(IncidentPriority.CRITICAL);
        assertThat(response.getAssetId()).isEqualTo("PROJ-LAB-3-V2");
        // Ensure status and active are preserved
        assertThat(response.getStatus()).isEqualTo(IncidentStatus.OPEN);
        assertThat(response.isActive()).isTrue();
        // Ensure reporterId is preserved
        assertThat(response.getReporterId()).isEqualTo("student-123");
    }

    @Test
    @DisplayName("updateIncident should throw ResourceNotFoundException when incident is inactive or missing")
    void updateIncident_shouldThrowWhenNotFoundOrInactive() {
        UpdateIncidentRequest updateRequest = UpdateIncidentRequest.builder()
                .title("Updated Title")
                .description("Updated Description")
                .priority(IncidentPriority.LOW)
                .build();

        when(incidentRepository.findByIdAndActiveTrue(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incidentService.updateIncident(999L, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Incident not found with id: 999");
    }
}
