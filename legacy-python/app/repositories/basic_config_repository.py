from dataclasses import dataclass
from typing import Any
from uuid import uuid4

from psycopg import Connection
from psycopg import sql
from psycopg.errors import UniqueViolation
from psycopg.types.json import Jsonb

from app.core.exceptions import DuplicateResourceError, ResourceNotFoundError


@dataclass(frozen=True)
class ConfigResource:
    name: str
    table: str
    key_field: str
    name_field: str
    list_fields: tuple[str, ...]
    json_fields: tuple[str, ...] = ()
    bool_fields: tuple[str, ...] = ("enabled",)
    default_values: dict[str, Any] | None = None


CONFIG_RESOURCES: dict[str, ConfigResource] = {
    "agent-configs": ConfigResource(
        name="agent-configs",
        table="agent_config",
        key_field="agent_key",
        name_field="agent_name",
        list_fields=(
            "id",
            "agent_key",
            "agent_name",
            "agent_type",
            "provider",
            "model_name",
            "system_prompt",
            "tool_permissions",
            "parameters",
            "enabled",
            "created_at",
            "updated_at",
        ),
        json_fields=("tool_permissions", "parameters"),
        default_values={"provider": "langgraph", "enabled": True},
    ),
    "integration-configs": ConfigResource(
        name="integration-configs",
        table="integration_config",
        key_field="integration_key",
        name_field="integration_name",
        list_fields=(
            "id",
            "integration_key",
            "integration_name",
            "integration_type",
            "base_url",
            "auth_type",
            "auth_config",
            "timeout_seconds",
            "enabled",
            "created_at",
            "updated_at",
        ),
        json_fields=("auth_config",),
        default_values={"auth_type": "none", "timeout_seconds": 10, "enabled": False},
    ),
    "message-templates": ConfigResource(
        name="message-templates",
        table="message_template",
        key_field="template_key",
        name_field="template_name",
        list_fields=(
            "id",
            "template_key",
            "template_name",
            "scene",
            "channel",
            "title_template",
            "body_template",
            "variables",
            "enabled",
            "created_at",
            "updated_at",
        ),
        json_fields=("variables",),
        default_values={"channel": "im", "enabled": True},
    ),
    "excel-field-mappings": ConfigResource(
        name="excel-field-mappings",
        table="excel_field_mapping",
        key_field="mapping_key",
        name_field="mapping_name",
        list_fields=(
            "id",
            "mapping_key",
            "mapping_name",
            "source_column",
            "target_field",
            "required",
            "transform_rule",
            "enabled",
            "created_at",
            "updated_at",
        ),
        json_fields=("transform_rule",),
        bool_fields=("required", "enabled"),
        default_values={"required": False, "enabled": True},
    ),
    "excel-import-templates": ConfigResource(
        name="excel-import-templates",
        table="excel_import_template",
        key_field="template_code",
        name_field="template_name",
        list_fields=(
            "id",
            "template_code",
            "template_name",
            "description",
            "handler_code",
            "source_columns",
            "entity_fields",
            "mapping_config",
            "enabled",
            "created_at",
            "updated_at",
        ),
        json_fields=("source_columns", "entity_fields", "mapping_config"),
        default_values={
            "handler_code": "standard_supervision",
            "source_columns": [],
            "entity_fields": [],
            "mapping_config": {},
            "enabled": True,
        },
    ),
    "reminder-rules": ConfigResource(
        name="reminder-rules",
        table="reminder_rule",
        key_field="rule_key",
        name_field="rule_name",
        list_fields=(
            "id",
            "rule_key",
            "rule_name",
            "trigger_type",
            "days_before_deadline",
            "repeat_interval_hours",
            "max_send_count",
            "skip_holidays",
            "enabled",
            "created_at",
            "updated_at",
        ),
        bool_fields=("skip_holidays", "enabled"),
        default_values={
            "days_before_deadline": 1,
            "repeat_interval_hours": 24,
            "max_send_count": 3,
            "skip_holidays": True,
            "enabled": True,
        },
    ),
    "status-dicts": ConfigResource(
        name="status-dicts",
        table="status_dict",
        key_field="status_key",
        name_field="status_name",
        list_fields=(
            "id",
            "status_key",
            "status_name",
            "status_group",
            "sort_order",
            "is_terminal",
            "enabled",
            "created_at",
            "updated_at",
        ),
        bool_fields=("is_terminal", "enabled"),
        default_values={
            "status_group": "supervision_item",
            "sort_order": 0,
            "is_terminal": False,
            "enabled": True,
        },
    ),
    "admin-menus": ConfigResource(
        name="admin-menus",
        table="admin_menu_config",
        key_field="menu_id",
        name_field="title",
        list_fields=(
            "id",
            "menu_id",
            "title",
            "icon",
            "menu_type",
            "resource",
            "hint",
            "group_name",
            "sort_order",
            "table_fields",
            "form_fields",
            "enabled",
            "created_at",
            "updated_at",
        ),
        json_fields=("table_fields", "form_fields"),
        default_values={
            "sort_order": 0,
            "table_fields": [],
            "form_fields": [],
            "enabled": True,
        },
    ),
}


def get_resource(resource_name: str) -> ConfigResource:
    try:
        return CONFIG_RESOURCES[resource_name]
    except KeyError as exc:
        raise ResourceNotFoundError(f"Unknown config resource: {resource_name}") from exc


def list_configs(conn: Connection, resource_name: str) -> list[dict[str, Any]]:
    resource = get_resource(resource_name)
    with conn.cursor() as cur:
        cur.execute(
            sql.SQL("SELECT {fields} FROM {table} ORDER BY updated_at DESC, created_at DESC").format(
                fields=sql.SQL(", ").join(sql.Identifier(field) for field in resource.list_fields),
                table=sql.Identifier(resource.table),
            )
        )
        return [normalize_row(row) for row in cur.fetchall()]


def get_config(conn: Connection, resource_name: str, item_id: str) -> dict[str, Any]:
    resource = get_resource(resource_name)
    with conn.cursor() as cur:
        cur.execute(
            sql.SQL("SELECT {fields} FROM {table} WHERE id = %s").format(
                fields=sql.SQL(", ").join(sql.Identifier(field) for field in resource.list_fields),
                table=sql.Identifier(resource.table),
            ),
            (item_id,),
        )
        row = cur.fetchone()
    if row is None:
        raise ResourceNotFoundError("config not found")
    return normalize_row(row)


def create_config(
    conn: Connection,
    resource_name: str,
    payload: dict[str, Any],
) -> dict[str, Any]:
    resource = get_resource(resource_name)
    values = normalize_payload(resource, payload, include_defaults=True)
    values["id"] = str(uuid4())

    fields = tuple(values)
    params = tuple(values[field] for field in fields)
    try:
        with conn.cursor() as cur:
            cur.execute(
                sql.SQL("INSERT INTO {table} ({fields}) VALUES ({placeholders})").format(
                    table=sql.Identifier(resource.table),
                    fields=sql.SQL(", ").join(sql.Identifier(field) for field in fields),
                    placeholders=sql.SQL(", ").join(sql.Placeholder() for _ in fields),
                ),
                params,
            )
        conn.commit()
    except UniqueViolation as exc:
        conn.rollback()
        raise DuplicateResourceError("duplicate config key") from exc

    return get_config(conn, resource_name, values["id"])


def update_config(
    conn: Connection,
    resource_name: str,
    item_id: str,
    payload: dict[str, Any],
) -> dict[str, Any]:
    resource = get_resource(resource_name)
    values = normalize_payload(resource, payload, include_defaults=False)
    if not values:
        return get_config(conn, resource_name, item_id)

    values["updated_at"] = sql.SQL("now()")
    assignments = []
    params = []
    for field, value in values.items():
        if field == "updated_at":
            assignments.append(sql.SQL("{} = now()").format(sql.Identifier(field)))
        else:
            assignments.append(
                sql.SQL("{} = {}").format(sql.Identifier(field), sql.Placeholder())
            )
            params.append(value)
    params.append(item_id)

    try:
        with conn.cursor() as cur:
            cur.execute(
                sql.SQL("UPDATE {table} SET {assignments} WHERE id = %s").format(
                    table=sql.Identifier(resource.table),
                    assignments=sql.SQL(", ").join(assignments),
                ),
                tuple(params),
            )
            if cur.rowcount == 0:
                conn.rollback()
                raise ResourceNotFoundError("config not found")
        conn.commit()
    except UniqueViolation as exc:
        conn.rollback()
        raise DuplicateResourceError("duplicate config key") from exc

    return get_config(conn, resource_name, item_id)


def delete_config(conn: Connection, resource_name: str, item_id: str) -> None:
    resource = get_resource(resource_name)
    with conn.cursor() as cur:
        cur.execute(
            sql.SQL("DELETE FROM {table} WHERE id = %s").format(
                table=sql.Identifier(resource.table),
            ),
            (item_id,),
        )
        if cur.rowcount == 0:
            conn.rollback()
            raise ResourceNotFoundError("config not found")
    conn.commit()


def normalize_payload(
    resource: ConfigResource,
    payload: dict[str, Any],
    include_defaults: bool,
) -> dict[str, Any]:
    allowed = set(resource.list_fields) - {"id", "created_at", "updated_at"}
    values: dict[str, Any] = {}
    if include_defaults and resource.default_values:
        values.update(resource.default_values)

    for key, value in payload.items():
        if key not in allowed:
            continue
        if key in resource.json_fields:
            values[key] = Jsonb(value if value is not None else ([] if key.endswith("s") else {}))
        else:
            values[key] = value
    return values


def normalize_row(row: dict[str, Any]) -> dict[str, Any]:
    return {
        key: value.isoformat() if hasattr(value, "isoformat") else value
        for key, value in row.items()
    }
