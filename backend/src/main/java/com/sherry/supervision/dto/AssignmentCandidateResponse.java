package com.sherry.supervision.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AssignmentCandidateResponse(
        @JsonProperty("assignee_user_id") String assigneeUserId,
        @JsonProperty("assignee_name") String assigneeName,
        @JsonProperty("department_id") String departmentId,
        @JsonProperty("department_name") String departmentName,
        @JsonProperty("role_type") String roleType,
        @JsonProperty("role_name") String roleName,
        double confidence,
        String reason,
        @JsonProperty("requires_human_review") boolean requiresHumanReview) {
}
