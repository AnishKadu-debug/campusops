package com.campusops.incident.service;

import com.campusops.incident.client.AssetServiceClient;
import com.campusops.incident.dto.request.AssignIncidentRequest;
import com.campusops.incident.dto.request.CreateIncidentRequest;
import com.campusops.incident.dto.request.UpdateIncidentRequest;
import com.campusops.incident.dto.request.UpdateIncidentStatusRequest;
import com.campusops.incident.entity.Incident;
import com.campusops.incident.entity.IncidentPriority;
import com.campusops.incident.entity.IncidentStatus;
import com.campusops.incident.exception.ForbiddenException;
import com.campusops.incident.repository.IncidentRepository;
import com.campusops.incident.security.CurrentUser;
import com.campusops.incident.security.UserRole;
import com.campusops.incident.service.impl.IncidentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentAuthorizationTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private AssetServiceClient assetServiceClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private SlaCalculator slaCalculator;

    @InjectMocks
    private IncidentServiceImpl incidentService;

    private CurrentUser studentA;
    private CurrentUser studentB;
    private CurrentUser technicianA;
    private CurrentUser technicianB;
    private CurrentUser manager;

    @BeforeEach
    void setUp() {
        studentA = new CurrentUser("student-A", UserRole.STUDENT);
        studentB = new CurrentUser("student-B", UserRole.STUDENT);
        technicianA = new CurrentUser("tech-A", UserRole.TECHNICIAN);
        technicianB = new CurrentUser("tech-B", UserRole.TECHNICIAN);
        manager = new CurrentUser("manager-1", UserRole.MANAGER);
    }

    private Incident incident(String reporterId, String assigneeId, IncidentStatus status) {
        return Incident.builder()
                .id(1L)
                .title("Broken projector")
                .description("Projector dead")
                .status(status)
                .priority(IncidentPriority.HIGH)
                .reporterId(reporterId)
                .assigneeId(assigneeId)
                .slaDeadline(Instant.now().plusSeconds(3600))
                .active(true)
                .build();
    }

    // ---------- CREATE ----------

    @Test
    @DisplayName("STUDENT create is allowed and reporter is forced to the JWT subject")
    void studentCreate_shouldForceReporterFromSubject() {
        CreateIncidentRequest request = CreateIncidentRequest.builder()
                .title("Broken AC")
                .description("Leaking")
                .priority(IncidentPriority.MEDIUM)
                .reporterId("someone-else") // must be ignored
                .build();

        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = incidentService.createIncident(request, studentA);

        assertThat(response.getReporterId()).isEqualTo("student-A");
    }

    @Test
    @DisplayName("TECHNICIAN create is forbidden")
    void technicianCreate_shouldBeForbidden() {
        CreateIncidentRequest request = CreateIncidentRequest.builder()
                .title("Broken AC").description("Leaking")
                .priority(IncidentPriority.MEDIUM).reporterId("tech-A").build();

        assertThatThrownBy(() -> incidentService.createIncident(request, technicianA))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(incidentRepository);
    }

    @Test
    @DisplayName("MANAGER create is forbidden")
    void managerCreate_shouldBeForbidden() {
        CreateIncidentRequest request = CreateIncidentRequest.builder()
                .title("Broken AC").description("Leaking")
                .priority(IncidentPriority.MEDIUM).reporterId("manager-1").build();

        assertThatThrownBy(() -> incidentService.createIncident(request, manager))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(incidentRepository);
    }

    // ---------- VIEW ----------

    @Test
    @DisplayName("STUDENT view own incident is allowed")
    void studentViewOwn_shouldBeAllowed() {
        when(incidentRepository.findByIdAndActiveTrue(1L))
                .thenReturn(Optional.of(incident("student-A", null, IncidentStatus.OPEN)));

        assertThat(incidentService.getIncidentById(1L, studentA)).isNotNull();
    }

    @Test
    @DisplayName("STUDENT view another student's incident is forbidden")
    void studentViewOther_shouldBeForbidden() {
        when(incidentRepository.findByIdAndActiveTrue(1L))
                .thenReturn(Optional.of(incident("student-B", null, IncidentStatus.OPEN)));

        assertThatThrownBy(() -> incidentService.getIncidentById(1L, studentA))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("TECHNICIAN view assigned incident is allowed")
    void technicianViewAssigned_shouldBeAllowed() {
        when(incidentRepository.findByIdAndActiveTrue(1L))
                .thenReturn(Optional.of(incident("student-A", "tech-A", IncidentStatus.ASSIGNED)));

        assertThat(incidentService.getIncidentById(1L, technicianA)).isNotNull();
    }

    @Test
    @DisplayName("TECHNICIAN view another technician's incident is forbidden")
    void technicianViewOtherTechnician_shouldBeForbidden() {
        when(incidentRepository.findByIdAndActiveTrue(1L))
                .thenReturn(Optional.of(incident("student-A", "tech-B", IncidentStatus.ASSIGNED)));

        assertThatThrownBy(() -> incidentService.getIncidentById(1L, technicianA))
                .isInstanceOf(ForbiddenException.class);
    }

    // ---------- UPDATE ----------

    @Test
    @DisplayName("STUDENT update own incident is allowed")
    void studentUpdateOwn_shouldBeAllowed() {
        UpdateIncidentRequest request = UpdateIncidentRequest.builder()
                .title("Updated").description("Updated desc")
                .priority(IncidentPriority.HIGH).build();

        when(incidentRepository.findByIdAndActiveTrue(1L))
                .thenReturn(Optional.of(incident("student-A", null, IncidentStatus.OPEN)));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(incidentService.updateIncident(1L, request, studentA)).isNotNull();
    }

    @Test
    @DisplayName("STUDENT update another student's incident is forbidden")
    void studentUpdateOther_shouldBeForbidden() {
        UpdateIncidentRequest request = UpdateIncidentRequest.builder()
                .title("Updated").description("Updated desc")
                .priority(IncidentPriority.HIGH).build();

        when(incidentRepository.findByIdAndActiveTrue(1L))
                .thenReturn(Optional.of(incident("student-B", null, IncidentStatus.OPEN)));

        assertThatThrownBy(() -> incidentService.updateIncident(1L, request, studentA))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("TECHNICIAN may not update incident details")
    void technicianUpdate_shouldBeForbidden() {
        UpdateIncidentRequest request = UpdateIncidentRequest.builder()
                .title("Updated").description("Updated desc")
                .priority(IncidentPriority.HIGH).build();

        when(incidentRepository.findByIdAndActiveTrue(1L))
                .thenReturn(Optional.of(incident("student-A", "tech-A", IncidentStatus.ASSIGNED)));

        assertThatThrownBy(() -> incidentService.updateIncident(1L, request, technicianA))
                .isInstanceOf(ForbiddenException.class);
    }

    // ---------- PROGRESS ----------

    @Test
    @DisplayName("TECHNICIAN progress assigned incident is allowed")
    void technicianProgressAssigned_shouldBeAllowed() {
        UpdateIncidentStatusRequest request = UpdateIncidentStatusRequest.builder()
                .status(IncidentStatus.IN_PROGRESS).build();

        when(incidentRepository.findByIdAndActiveTrue(1L))
                .thenReturn(Optional.of(incident("student-A", "tech-A", IncidentStatus.ASSIGNED)));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(incidentService.updateIncidentStatus(1L, request, technicianA)).isNotNull();
    }

    @Test
    @DisplayName("TECHNICIAN progress unassigned incident is forbidden")
    void technicianProgressUnassigned_shouldBeForbidden() {
        UpdateIncidentStatusRequest request = UpdateIncidentStatusRequest.builder()
                .status(IncidentStatus.IN_PROGRESS).build();

        when(incidentRepository.findByIdAndActiveTrue(1L))
                .thenReturn(Optional.of(incident("student-A", null, IncidentStatus.OPEN)));

        assertThatThrownBy(() -> incidentService.updateIncidentStatus(1L, request, technicianA))
                .isInstanceOf(ForbiddenException.class);
    }

    // ---------- LIST SCOPING ----------

    @Test
    @DisplayName("MANAGER list returns all active incidents")
    void managerList_shouldReturnAll() {
        when(incidentRepository.findByActiveTrue()).thenReturn(List.of());

        assertThat(incidentService.getAllIncidents(null, manager)).isEmpty();
    }

    @Test
    @DisplayName("STUDENT list is scoped to their own incidents")
    void studentList_shouldBeScopedToReporter() {
        when(incidentRepository.findByReporterIdAndActiveTrue("student-A")).thenReturn(List.of());

        incidentService.getAllIncidents(null, studentA);

        verifyScopedQuery();
    }

    @Test
    @DisplayName("TECHNICIAN list is scoped to incidents assigned to them")
    void technicianList_shouldBeScopedToAssignee() {
        when(incidentRepository.findByAssigneeIdAndActiveTrue("tech-A")).thenReturn(List.of());

        incidentService.getAllIncidents(null, technicianA);

        org.mockito.Mockito.verify(incidentRepository).findByAssigneeIdAndActiveTrue("tech-A");
    }

    private void verifyScopedQuery() {
        org.mockito.Mockito.verify(incidentRepository).findByReporterIdAndActiveTrue("student-A");
    }
}
