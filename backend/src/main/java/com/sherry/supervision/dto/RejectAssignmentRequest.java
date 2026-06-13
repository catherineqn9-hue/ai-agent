package com.sherry.supervision.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectAssignmentRequest(
        @JsonProperty("rejection_reason")
        @NotBlank
        @Size(max = 1000)
        String rejectionReason) {
}
