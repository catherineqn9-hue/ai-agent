package com.sherry.supervision.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sherry.supervision.entity.AiToolCallLog;
import com.sherry.supervision.mapper.AiToolCallLogMapper;
import com.sherry.supervision.service.impl.AiToolCallLogServiceImpl;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AiToolCallLogServiceTest {

    private AiToolCallLogMapper mapper;
    private AiToolCallLogService service;

    @BeforeEach
    void setUp() {
        mapper = mock(AiToolCallLogMapper.class);
        service = new AiToolCallLogServiceImpl(mapper, new ObjectMapper());
    }

    @Test
    void shouldRecordSuccessLogWithJsonPayload() {
        service.recordSuccess(
                "request-1",
                "thread-1",
                "supervision_assistant",
                "query_import_batches",
                Map.of("keyword", "excel"),
                Map.of("count", 2),
                15L);

        ArgumentCaptor<AiToolCallLog> captor = ArgumentCaptor.forClass(AiToolCallLog.class);
        verify(mapper).insert(captor.capture());
        AiToolCallLog log = captor.getValue();
        assertThat(log.getId()).isNotNull();
        assertThat(log.getRequestId()).isEqualTo("request-1");
        assertThat(log.getThreadId()).isEqualTo("thread-1");
        assertThat(log.getAgentKey()).isEqualTo("supervision_assistant");
        assertThat(log.getToolName()).isEqualTo("query_import_batches");
        assertThat(log.getStatus()).isEqualTo("success");
        assertThat(log.getInputPayload().get("keyword").asText()).isEqualTo("excel");
        assertThat(log.getOutputPayload().get("count").asInt()).isEqualTo(2);
        assertThat(log.getDurationMs()).isEqualTo(15);
        assertThat(log.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldRecordFailureLogWithErrorMessage() {
        service.recordFailure(
                "request-2",
                "thread-2",
                "supervision_assistant",
                "update_supervision_status",
                Map.of("status", "bad"),
                "状态流转不合法",
                8L);

        ArgumentCaptor<AiToolCallLog> captor = ArgumentCaptor.forClass(AiToolCallLog.class);
        verify(mapper).insert(captor.capture());
        AiToolCallLog log = captor.getValue();
        assertThat(log.getStatus()).isEqualTo("failed");
        assertThat(log.getErrorMessage()).isEqualTo("状态流转不合法");
        assertThat(log.getOutputPayload().isObject()).isTrue();
        assertThat(log.getInputPayload().get("status").asText()).isEqualTo("bad");
    }
}
