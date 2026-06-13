package com.sherry.supervision.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sherry.supervision.entity.ItemAssignee;
import com.sherry.supervision.entity.SupervisionItem;

public record MySupervisionItem(
        SupervisionItem item,
        ItemAssignee assignment,
        @JsonProperty("risk_level") String riskLevel,
        @JsonProperty("requires_human_review") boolean requiresHumanReview) {
}
