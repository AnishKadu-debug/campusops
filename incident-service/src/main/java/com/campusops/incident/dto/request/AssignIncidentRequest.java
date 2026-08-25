package com.campusops.incident.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class AssignIncidentRequest {

    @NotBlank(message = "Assignee ID is required")
    @Size(max = 100, message = "Assignee ID must not exceed 100 characters")
    private String assigneeId;
}
