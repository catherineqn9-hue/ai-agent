package com.sherry.supervision.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record ProgressFeedbackRequest(
        UUID itemId,
        String feedbackUserId,
        String feedbackUserName,
        @Min(0) @Max(100) Integer progressPercent,
        @NotBlank String content,
        String riskNote) {
}
