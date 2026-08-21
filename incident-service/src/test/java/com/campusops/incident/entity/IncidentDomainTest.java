package com.campusops.incident.entity;

import com.campusops.incident.dto.response.IncidentResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class IncidentDomainTest {

    @Test
    @DisplayName("Incident entity and IncidentResponse should map all fields accurately")
    void incidentEntity_andResponseMapping() {
        Instant now = Instant.now();
        Incident incident = Incident.builder()
                .id(10L)
                .title("Damaged Oscilloscope")
                .description("Channel 1 input is faulty")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.HIGH)
                .assetId("OSC-LAB-01")
                .reporterId("faculty-01")
                .assigneeId("tech-05")
                .active(true)
                .build();
        incident.setCreatedAt(now);
        incident.setUpdatedAt(now);

        IncidentResponse response = IncidentResponse.fromEntity(incident);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("Damaged Oscilloscope");
        assertThat(response.getDescription()).isEqualTo("Channel 1 input is faulty");
        assertThat(response.getStatus()).isEqualTo(IncidentStatus.OPEN);
        assertThat(response.getPriority()).isEqualTo(IncidentPriority.HIGH);
        assertThat(response.getAssetId()).isEqualTo("OSC-LAB-01");
        assertThat(response.getReporterId()).isEqualTo("faculty-01");
        assertThat(response.getAssigneeId()).isEqualTo("tech-05");
        assertThat(response.isActive()).isTrue();
        assertThat(response.getCreatedAt()).isEqualTo(now);
        assertThat(response.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("IncidentResponse from null entity should return null")
    void incidentResponse_fromNullEntity_shouldReturnNull() {
        assertThat(IncidentResponse.fromEntity(null)).isNull();
    }

    @Test
    @DisplayName("Enums should contain expected values per specifications")
    void enumValues_shouldMatchSpecifications() {
        assertThat(IncidentStatus.values()).containsExactly(
                IncidentStatus.OPEN,
                IncidentStatus.ASSIGNED,
                IncidentStatus.IN_PROGRESS,
                IncidentStatus.RESOLVED,
                IncidentStatus.CLOSED
        );

        assertThat(IncidentPriority.values()).containsExactly(
                IncidentPriority.LOW,
                IncidentPriority.MEDIUM,
                IncidentPriority.HIGH,
                IncidentPriority.CRITICAL
        );
    }
}
