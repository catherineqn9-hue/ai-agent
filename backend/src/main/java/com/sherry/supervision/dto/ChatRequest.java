package com.sherry.supervision.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record ChatRequest(
        @NotBlank String userId,
        @NotBlank String message,
        String threadId,
        Map<String, Object> context) {
}
