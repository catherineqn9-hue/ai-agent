package com.sherry.supervision.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.sherry.supervision.configresource.ConfigResourceRegistry;
import com.sherry.supervision.exception.InvalidRequestException;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class BasicConfigServiceTest {

    private BasicConfigService service;

    @BeforeEach
    void setUp() {
        service = new BasicConfigService(mock(JdbcTemplate.class), new ConfigResourceRegistry());
    }

    @Test
    void shouldRejectCreateForReadonlyResource() {
        assertThatThrownBy(() -> service.create("ai-tool-call-logs", Map.of()))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("只读");
    }

    @Test
    void shouldRejectUpdateForReadonlyResource() {
        assertThatThrownBy(() -> service.update("ai-tool-call-logs", UUID.randomUUID(), Map.of()))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("只读");
    }

    @Test
    void shouldRejectDeleteForReadonlyResource() {
        assertThatThrownBy(() -> service.delete("ai-tool-call-logs", UUID.randomUUID()))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("只读");
    }

    @Test
    void shouldListReadonlyLogResourceWithoutUpdatedAtOrdering() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        service = new BasicConfigService(jdbcTemplate, new ConfigResourceRegistry());

        service.list("ai-tool-call-logs");

        verify(jdbcTemplate).query(
                eq("SELECT id, request_id, thread_id, agent_key, tool_name, input_payload, output_payload, status, error_message, duration_ms, created_at FROM ai_tool_call_log ORDER BY created_at DESC"),
                any(RowMapper.class));
    }
}
