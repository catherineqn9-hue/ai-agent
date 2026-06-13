package com.sherry.supervision.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ManagedUserResponse(
        String id,
        String username,
        @JsonProperty("display_name") String displayName,
        @JsonProperty("department_id") String departmentId,
        @JsonProperty("department_name") String departmentName,
        @JsonProperty("role_key") String roleKey,
        @JsonProperty("role_name") String roleName,
        Boolean enabled) {
}
