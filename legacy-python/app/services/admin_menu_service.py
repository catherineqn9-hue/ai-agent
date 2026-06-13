from typing import Any

from app.db import get_connection


def list_admin_menus() -> list[dict[str, Any]]:
    with get_connection() as conn:
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT
                    menu_id,
                    title,
                    icon,
                    menu_type,
                    resource,
                    hint,
                    group_name,
                    table_fields,
                    form_fields
                FROM admin_menu_config
                WHERE enabled = true
                ORDER BY sort_order ASC, created_at ASC
                """
            )
            return [to_menu(row) for row in cur.fetchall()]


def to_menu(row: dict[str, Any]) -> dict[str, Any]:
    return {
        "id": row["menu_id"],
        "title": row["title"],
        "icon": row["icon"],
        "type": row["menu_type"],
        "resource": row["resource"],
        "hint": row["hint"],
        "groupName": row["group_name"],
        "tableFields": row["table_fields"] or [],
        "fields": row["form_fields"] or [],
    }
