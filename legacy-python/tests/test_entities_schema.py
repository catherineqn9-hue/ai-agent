from pathlib import Path

from app.entities import AgentConfigEntity, ItemStatus, SupervisionItemEntity


def test_schema_sql_contains_core_tables():
    schema = Path("database/schema.sql").read_text(encoding="utf-8")

    for table in [
        "agent_config",
        "integration_config",
        "message_template",
        "excel_import_template",
        "supervision_batch",
        "supervision_item",
        "item_assignee",
        "progress_feedback",
        "reminder_record",
        "ai_tool_call_log",
        "admin_menu_config",
    ]:
        assert f"CREATE TABLE IF NOT EXISTS {table}" in schema


def test_entity_fields_match_core_concepts():
    agent_fields = set(AgentConfigEntity.model_fields)
    item_fields = set(SupervisionItemEntity.model_fields)

    assert {"agent_key", "provider", "system_prompt", "tool_permissions"}.issubset(agent_fields)
    assert {"item_no", "title", "status", "deadline_at", "created_by"}.issubset(item_fields)
    assert ItemStatus.PENDING_ASSIGN == "pending_assign"
