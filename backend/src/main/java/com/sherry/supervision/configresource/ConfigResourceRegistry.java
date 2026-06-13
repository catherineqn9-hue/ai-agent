package com.sherry.supervision.configresource;

import com.sherry.supervision.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ConfigResourceRegistry {

    private final Map<String, ConfigResource> resources = Map.of(
            "agent-configs",
            new ConfigResource(
                    "agent-configs",
                    "agent_config",
                    "agent_key",
                    "agent_name",
                    List.of(
                            "id", "agent_key", "agent_name", "agent_type", "provider", "model_name",
                            "system_prompt", "tool_permissions", "parameters", "enabled", "created_at", "updated_at"),
                    Set.of("tool_permissions", "parameters"),
                    Map.of("provider", "langgraph", "enabled", true)),
            "integration-configs",
            new ConfigResource(
                    "integration-configs",
                    "integration_config",
                    "integration_key",
                    "integration_name",
                    List.of(
                            "id", "integration_key", "integration_name", "integration_type", "base_url",
                            "auth_type", "auth_config", "timeout_seconds", "enabled", "created_at", "updated_at"),
                    Set.of("auth_config"),
                    Map.of("auth_type", "none", "timeout_seconds", 10, "enabled", false)),
            "message-templates",
            new ConfigResource(
                    "message-templates",
                    "message_template",
                    "template_key",
                    "template_name",
                    List.of(
                            "id", "template_key", "template_name", "scene", "channel", "title_template",
                            "body_template", "variables", "enabled", "created_at", "updated_at"),
                    Set.of("variables"),
                    Map.of("channel", "im", "enabled", true)),
            "excel-import-templates",
            new ConfigResource(
                    "excel-import-templates",
                    "excel_import_template",
                    "template_code",
                    "template_name",
                    List.of(
                            "id", "template_code", "template_name", "description", "handler_code",
                            "source_columns", "entity_fields", "mapping_config", "enabled", "created_at", "updated_at"),
                    Set.of("source_columns", "entity_fields", "mapping_config"),
                    Map.of(
                            "handler_code", "standard_supervision",
                            "source_columns", List.of(),
                            "entity_fields", List.of(),
                            "mapping_config", Map.of(),
                            "enabled", true)),
            "reminder-rules",
            new ConfigResource(
                    "reminder-rules",
                    "reminder_rule",
                    "rule_key",
                    "rule_name",
                    List.of(
                            "id", "rule_key", "rule_name", "trigger_type", "days_before_deadline",
                            "repeat_interval_hours", "max_send_count", "skip_holidays", "enabled", "created_at", "updated_at"),
                    Set.of(),
                    Map.of(
                            "days_before_deadline", 1,
                            "repeat_interval_hours", 24,
                            "max_send_count", 3,
                            "skip_holidays", true,
                            "enabled", true)),
            "status-dicts",
            new ConfigResource(
                    "status-dicts",
                    "status_dict",
                    "status_key",
                    "status_name",
                    List.of("id", "status_key", "status_name", "status_group", "sort_order", "is_terminal", "enabled", "created_at", "updated_at"),
                    Set.of(),
                    Map.of("status_group", "supervision_item", "sort_order", 0, "is_terminal", false, "enabled", true)),
            "admin-menus",
            new ConfigResource(
                    "admin-menus",
                    "admin_menu_config",
                    "menu_id",
                    "title",
                    List.of(
                            "id", "menu_id", "title", "icon", "menu_type", "resource", "hint",
                            "group_name", "sort_order", "table_fields", "form_fields", "enabled", "created_at", "updated_at"),
                    Set.of("table_fields", "form_fields"),
                    Map.of("sort_order", 0, "table_fields", List.of(), "form_fields", List.of(), "enabled", true)),
            "ai-tool-call-logs",
            new ConfigResource(
                    "ai-tool-call-logs",
                    "ai_tool_call_log",
                    "request_id",
                    "tool_name",
                    List.of(
                            "id", "request_id", "thread_id", "agent_key", "tool_name",
                            "input_payload", "output_payload", "status", "error_message",
                            "duration_ms", "created_at"),
                    Set.of("input_payload", "output_payload"),
                    Map.of(),
                    true));

    public ConfigResource get(String name) {
        ConfigResource resource = resources.get(name);
        if (resource == null) {
            throw new ResourceNotFoundException("配置资源不存在：" + name);
        }
        return resource;
    }

    public List<ConfigResource> list() {
        return resources.values().stream().toList();
    }
}
