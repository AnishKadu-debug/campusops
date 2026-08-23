package com.campusops.incident.dto.request;

import com.campusops.incident.entity.IncidentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateIncidentStatusRequest {

    @NotNull(message = "Status is required")
    private IncidentStatus status;
}
