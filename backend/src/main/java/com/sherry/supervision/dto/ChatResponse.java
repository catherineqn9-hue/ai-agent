package com.sherry.supervision.dto;

import java.util.List;
import java.util.Map;

public record ChatResponse(
        String requestId,
        String threadId,
        String answer,
        boolean needClarification,
        List<String> questions,
        String intent,
        Map<String, Object> slots,
        String status,
        String traceId,
        List<Map<String, String>> trace) {
}
