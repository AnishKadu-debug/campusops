package com.campusops.incident.controller;

import com.campusops.incident.dto.request.CreateIncidentRequest;
import com.campusops.incident.dto.request.UpdateIncidentRequest;
import com.campusops.incident.dto.response.IncidentResponse;
import com.campusops.incident.entity.IncidentPriority;
import com.campusops.incident.entity.IncidentStatus;
import com.campusops.incident.exception.GlobalExceptionHandler;
import com.campusops.incident.exception.ResourceNotFoundException;
import com.campusops.incident.service.IncidentService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class IncidentControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private IncidentService incidentService;

    @InjectMocks
    private IncidentController incidentController;

    private IncidentResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(incidentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        sampleResponse = IncidentResponse.builder()
                .id(1L)
                .title("Wi-Fi down in Library")
                .description("No access point connection on 2nd floor")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.HIGH)
                .assetId("ROUTER-LIB-02")
                .reporterId("student-99")
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/incidents with valid payload should return 201 Created")
    void createIncident_withValidPayload_shouldReturn201() throws Exception {
        CreateIncidentRequest request = CreateIncidentRequest.builder()
                .title("Wi-Fi down in Library")
                .description("No access point connection on 2nd floor")
                .priority(IncidentPriority.HIGH)
                .assetId("ROUTER-LIB-02")
                .reporterId("student-99")
                .build();

        when(incidentService.createIncident(any(CreateIncidentRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Wi-Fi down in Library")))
                .andExpect(jsonPath("$.status", is("OPEN")))
                .andExpect(jsonPath("$.priority", is("HIGH")))
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    @DisplayName("POST /api/v1/incidents with blank fields should return 400 Bad Request with validation errors")
    void createIncident_withInvalidPayload_shouldReturn400() throws Exception {
        CreateIncidentRequest invalidRequest = CreateIncidentRequest.builder()
                .title("") // Blank title
                .description("") // Blank description
                .priority(null) // Null priority
                .reporterId("") // Blank reporterId
                .build();

        mockMvc.perform(post("/api/v1/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.validationErrors.title").exists())
                .andExpect(jsonPath("$.validationErrors.description").exists())
                .andExpect(jsonPath("$.validationErrors.priority").exists())
                .andExpect(jsonPath("$.validationErrors.reporterId").exists());
    }

    @Test
    @DisplayName("GET /api/v1/incidents should return 200 OK with list of incidents")
    void getAllIncidents_shouldReturn200() throws Exception {
        when(incidentService.getAllIncidents(null)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/incidents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].title", is("Wi-Fi down in Library")));
    }

    @Test
    @DisplayName("GET /api/v1/incidents/{id} with existing active ID should return 200 OK")
    void getIncidentById_whenFound_shouldReturn200() throws Exception {
        when(incidentService.getIncidentById(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/incidents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Wi-Fi down in Library")));
    }

    @Test
    @DisplayName("GET /api/v1/incidents/{id} when missing or inactive should return 404 Not Found")
    void getIncidentById_whenNotFound_shouldReturn404() throws Exception {
        when(incidentService.getIncidentById(999L))
                .thenThrow(new ResourceNotFoundException("Incident not found with id: 999"));

        mockMvc.perform(get("/api/v1/incidents/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Incident not found with id: 999")));
    }

    @Test
    @DisplayName("PUT /api/v1/incidents/{id} with valid payload should return 200 OK")
    void updateIncident_withValidPayload_shouldReturn200() throws Exception {
        UpdateIncidentRequest updateRequest = UpdateIncidentRequest.builder()
                .title("Wi-Fi completely down in entire Library")
                .description("Switches power cycled but not connecting")
                .priority(IncidentPriority.CRITICAL)
                .assetId("ROUTER-LIB-02")
                .build();

        IncidentResponse updatedResponse = IncidentResponse.builder()
                .id(1L)
                .title("Wi-Fi completely down in entire Library")
                .description("Switches power cycled but not connecting")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.CRITICAL)
                .assetId("ROUTER-LIB-02")
                .reporterId("student-99")
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(incidentService.updateIncident(eq(1L), any(UpdateIncidentRequest.class))).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/v1/incidents/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Wi-Fi completely down in entire Library")))
                .andExpect(jsonPath("$.priority", is("CRITICAL")));
    }

    @Test
    @DisplayName("PUT /api/v1/incidents/{id} with missing fields should return 400 Bad Request")
    void updateIncident_withInvalidPayload_shouldReturn400() throws Exception {
        UpdateIncidentRequest invalidRequest = UpdateIncidentRequest.builder()
                .title("")
                .description(null)
                .priority(null)
                .build();

        mockMvc.perform(put("/api/v1/incidents/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors.title").exists())
                .andExpect(jsonPath("$.validationErrors.description").exists())
                .andExpect(jsonPath("$.validationErrors.priority").exists());
    }
}
