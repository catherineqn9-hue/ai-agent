package com.sherry.supervision.service;

public interface AiToolCallLogService {

    void recordSuccess(
            String requestId,
            String threadId,
            String agentKey,
            String toolName,
            Object inputPayload,
            Object outputPayload,
            Long durationMs);

    void recordFailure(
            String requestId,
            String threadId,
            String agentKey,
            String toolName,
            Object inputPayload,
            String errorMessage,
            Long durationMs);
}
