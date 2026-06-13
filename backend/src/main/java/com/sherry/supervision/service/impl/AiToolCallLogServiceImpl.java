package com.sherry.supervision.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sherry.supervision.entity.AiToolCallLog;
import com.sherry.supervision.mapper.AiToolCallLogMapper;
import com.sherry.supervision.service.AiToolCallLogService;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AiToolCallLogServiceImpl implements AiToolCallLogService {

    private final AiToolCallLogMapper mapper;
    private final ObjectMapper objectMapper;

    public AiToolCallLogServiceImpl(AiToolCallLogMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public void recordSuccess(
            String requestId,
            String threadId,
            String agentKey,
            String toolName,
            Object inputPayload,
            Object outputPayload,
            Long durationMs) {
        AiToolCallLog log = baseLog(requestId, threadId, agentKey, toolName, inputPayload, durationMs);
        log.setStatus("success");
        log.setOutputPayload(objectMapper.valueToTree(outputPayload == null ? Map.of() : outputPayload));
        mapper.insert(log);
    }

    @Override
    public void recordFailure(
            String requestId,
            String threadId,
            String agentKey,
            String toolName,
            Object inputPayload,
            String errorMessage,
            Long durationMs) {
        AiToolCallLog log = baseLog(requestId, threadId, agentKey, toolName, inputPayload, durationMs);
        log.setStatus("failed");
        log.setOutputPayload(objectMapper.createObjectNode());
        log.setErrorMessage(errorMessage);
        mapper.insert(log);
    }

    private AiToolCallLog baseLog(
            String requestId,
            String threadId,
            String agentKey,
            String toolName,
            Object inputPayload,
            Long durationMs) {
        AiToolCallLog log = new AiToolCallLog();
        log.setId(UUID.randomUUID());
        log.setRequestId(requestId);
        log.setThreadId(threadId);
        log.setAgentKey(agentKey);
        log.setToolName(toolName);
        log.setInputPayload(objectMapper.valueToTree(inputPayload == null ? Map.of() : inputPayload));
        log.setDurationMs(durationMs == null ? null : Math.toIntExact(durationMs));
        log.setCreatedAt(OffsetDateTime.now());
        return log;
    }
}
