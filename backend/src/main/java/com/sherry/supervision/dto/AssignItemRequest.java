package com.sherry.supervision.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssignItemRequest(
        @JsonProperty("assignee_user_id")
        @NotBlank
        @Size(max = 80)
        String assigneeUserId,

        @JsonProperty("assignee_name")
        @NotBlank
        @Size(max = 120)
        String assigneeName,

        @JsonProperty("role_type")
        @Size(max = 40)
        String roleType,

        @JsonProperty("department_id")
        @Size(max = 80)
        String departmentId,

        @JsonProperty("department_name")
        @Size(max = 120)
        String departmentName,

        @JsonProperty("assignment_note")
        @Size(max = 1000)
        String assignmentNote) {
}
