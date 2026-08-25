package com.campusops.notification.controller;

import com.campusops.notification.dto.response.NotificationResponse;
import com.campusops.notification.entity.NotificationEventType;
import com.campusops.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private MockMvc mockMvc;

    private NotificationResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController).build();
        sampleResponse = NotificationResponse.builder()
                .id(1L)
                .incidentId(41L)
                .eventType(NotificationEventType.INCIDENT_ASSIGNED)
                .recipientId("tech-9")
                .message("Incident 'Projector not working' has been assigned to you.")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/notifications should return all notifications")
    void getNotifications_shouldReturnAll() throws Exception {
        when(notificationService.getNotificationHistory(null)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].incidentId", is(41)))
                .andExpect(jsonPath("$[0].eventType", is("INCIDENT_ASSIGNED")))
                .andExpect(jsonPath("$[0].recipientId", is("tech-9")));
    }

    @Test
    @DisplayName("GET /api/v1/notifications?incidentId= should filter by incident")
    void getNotifications_withIncidentId_shouldFilter() throws Exception {
        when(notificationService.getNotificationHistory(41L)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/notifications").param("incidentId", "41"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].incidentId", is(41)));
    }

    @Test
    @DisplayName("GET /api/v1/notifications should return empty list when no notifications exist")
    void getNotifications_emptyHistory_shouldReturnEmptyList() throws Exception {
        when(notificationService.getNotificationHistory(null)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
