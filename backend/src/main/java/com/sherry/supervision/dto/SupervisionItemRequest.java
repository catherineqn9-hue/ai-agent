package com.sherry.supervision.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;

public record SupervisionItemRequest(
        String itemNo,
        @NotBlank String title,
        String description,
        String priority,
        String status,
        OffsetDateTime deadlineAt,
        String createdBy) {
}
