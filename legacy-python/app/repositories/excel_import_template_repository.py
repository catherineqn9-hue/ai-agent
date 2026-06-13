from typing import Any

from psycopg import Connection

from app.core.exceptions import ResourceNotFoundError


def list_enabled_templates(conn: Connection) -> list[dict[str, Any]]:
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT
                template_code,
                template_name,
                description,
                handler_code,
                source_columns,
                entity_fields,
                mapping_config
            FROM excel_import_template
            WHERE enabled = true
            ORDER BY template_name ASC, created_at ASC
            """
        )
        return cur.fetchall()


def get_enabled_template(conn: Connection, template_code: str) -> dict[str, Any]:
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT
                template_code,
                template_name,
                description,
                handler_code,
                source_columns,
                entity_fields,
                mapping_config
            FROM excel_import_template
            WHERE template_code = %s AND enabled = true
            """,
            (template_code,),
        )
        row = cur.fetchone()
    if row is None:
        raise ResourceNotFoundError("excel import template not found")
    return row
