package com.campusops.incident.api;

import com.campusops.incident.config.KafkaTestConfig;
import com.campusops.incident.entity.Incident;
import com.campusops.incident.entity.IncidentPriority;
import com.campusops.incident.entity.IncidentStatus;
import com.campusops.incident.repository.IncidentRepository;
import com.campusops.incident.security.dev.DevTokenGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(KafkaTestConfig.class)
@Transactional
class IncidentApiSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Incident incidentOfStudentB;

    @BeforeEach
    void seed() {
        incidentOfStudentB = incidentRepository.save(Incident.builder()
                .title("Broken projector in Lab 5")
                .description("Owned by student-B")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.HIGH)
                .reporterId("student-B")
                .slaDeadline(Instant.now().plusSeconds(3600))
                .active(true)
                .build());
    }

    private String token(String role, String subject) {
        return DevTokenGenerator.mint(DevTokenGenerator.DEV_SECRET, subject, role);
    }

    @Test
    @DisplayName("request without JWT should return 401")
    void noToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/incidents"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("request with invalid JWT should return 401")
    void invalidToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/incidents")
                        .header("Authorization", "Bearer this.is.not.a.valid.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("STUDENT create should force reporterId from the JWT subject, ignoring the body value")
    void studentCreate_shouldForceReporterFromJwt() throws Exception {
        String body = """
                {
                  "title": "Wi-Fi down",
                  "description": "Library second floor",
                  "priority": "HIGH",
                  "reporterId": "someone-else"
                }
                """;

        mockMvc.perform(post("/api/v1/incidents")
                        .header("Authorization", "Bearer " + token("STUDENT", "student-A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reporterId", is("student-A")));
    }

    @Test
    @DisplayName("STUDENT viewing another student's incident should return 403")
    void studentViewingOthersIncident_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/v1/incidents/" + incidentOfStudentB.getId())
                        .header("Authorization", "Bearer " + token("STUDENT", "student-A")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("MANAGER viewing any incident should return 200")
    void managerViewingAnyIncident_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/incidents/" + incidentOfStudentB.getId())
                        .header("Authorization", "Bearer " + token("MANAGER", "manager-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reporterId", is("student-B")));
    }

    @Test
    @DisplayName("TECHNICIAN calling assign endpoint should return 403")
    void technicianAssign_shouldReturn403() throws Exception {
        mockMvc.perform(patch("/api/v1/incidents/" + incidentOfStudentB.getId() + "/assign")
                        .header("Authorization", "Bearer " + token("TECHNICIAN", "tech-A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":\"tech-A\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("STUDENT calling assign endpoint should return 403")
    void studentAssign_shouldReturn403() throws Exception {
        mockMvc.perform(patch("/api/v1/incidents/" + incidentOfStudentB.getId() + "/assign")
                        .header("Authorization", "Bearer " + token("STUDENT", "student-A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":\"tech-A\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("MANAGER assigning a technician should succeed")
    void managerAssign_shouldSucceed() throws Exception {
        mockMvc.perform(patch("/api/v1/incidents/" + incidentOfStudentB.getId() + "/assign")
                        .header("Authorization", "Bearer " + token("MANAGER", "manager-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":\"tech-x\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ASSIGNED")))
                .andExpect(jsonPath("$.assigneeId", is("tech-x")));
    }
}
