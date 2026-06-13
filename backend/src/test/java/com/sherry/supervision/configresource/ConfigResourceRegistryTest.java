package com.sherry.supervision.configresource;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConfigResourceRegistryTest {

    private final ConfigResourceRegistry registry = new ConfigResourceRegistry();

    @Test
    void shouldExposeAiToolCallLogsAsReadonlyResource() {
        ConfigResource resource = registry.get("ai-tool-call-logs");

        assertThat(resource.tableName()).isEqualTo("ai_tool_call_log");
        assertThat(resource.readOnly()).isTrue();
        assertThat(resource.fields())
                .contains(
                        "request_id",
                        "thread_id",
                        "agent_key",
                        "tool_name",
                        "input_payload",
                        "output_payload",
                        "status",
                        "error_message",
                        "duration_ms",
                        "created_at");
        assertThat(resource.jsonFields()).contains("input_payload", "output_payload");
    }
}
