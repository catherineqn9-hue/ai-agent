package com.sherry.supervision.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateManagedUserRequest(
        @JsonProperty("display_name")
        @NotBlank
        @Size(max = 120)
        String displayName,

        @JsonProperty("department_id")
        @NotBlank
        @Size(max = 80)
        String departmentId,

        @JsonProperty("department_name")
        @NotBlank
        @Size(max = 120)
        String departmentName,

        @JsonProperty("role_key")
        @NotBlank
        @Size(max = 60)
        String roleKey,

        @JsonProperty("role_name")
        @NotBlank
        @Size(max = 120)
        String roleName,

        Boolean enabled) {
}
