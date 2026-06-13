from typing import Any
from uuid import uuid4

from psycopg import Connection
from psycopg import sql
from psycopg.types.json import Jsonb

from app.core.exceptions import ResourceNotFoundError


LIST_FIELDS = (
    "id",
    "item_id",
    "assignee_id",
    "feedback_user_id",
    "feedback_user_name",
    "progress_percent",
    "content",
    "risk_note",
    "attachment_ids",
    "feedback_at",
    "created_at",
)


def list_feedbacks(conn: Connection, item_id: str | None = None) -> list[dict[str, Any]]:
    where_clause = sql.SQL("")
    params: tuple[Any, ...] = ()
    if item_id:
        where_clause = sql.SQL("WHERE item_id = %s")
        params = (item_id,)

    with conn.cursor() as cur:
        cur.execute(
            sql.SQL(
                "SELECT {fields} FROM progress_feedback {where_clause} "
                "ORDER BY feedback_at DESC, created_at DESC"
            ).format(
                fields=sql.SQL(", ").join(sql.Identifier(field) for field in LIST_FIELDS),
                where_clause=where_clause,
            ),
            params,
        )
        return [normalize_row(row) for row in cur.fetchall()]


def create_feedback(conn: Connection, payload: dict[str, Any]) -> dict[str, Any]:
    feedback_id = str(uuid4())
    values = {
        "id": feedback_id,
        "item_id": payload["item_id"],
        "feedback_user_id": payload["feedback_user_id"],
        "feedback_user_name": payload["feedback_user_name"],
        "progress_percent": payload["progress_percent"],
        "content": payload["content"],
        "risk_note": payload.get("risk_note"),
        "attachment_ids": Jsonb(payload.get("attachment_ids") or []),
    }
    fields = tuple(values)

    with conn.cursor() as cur:
        cur.execute(
            sql.SQL("INSERT INTO progress_feedback ({fields}) VALUES ({placeholders})").format(
                fields=sql.SQL(", ").join(sql.Identifier(field) for field in fields),
                placeholders=sql.SQL(", ").join(sql.Placeholder() for _ in fields),
            ),
            tuple(values[field] for field in fields),
        )
    conn.commit()
    return get_feedback(conn, feedback_id)


def get_feedback(conn: Connection, feedback_id: str) -> dict[str, Any]:
    with conn.cursor() as cur:
        cur.execute(
            sql.SQL("SELECT {fields} FROM progress_feedback WHERE id = %s").format(
                fields=sql.SQL(", ").join(sql.Identifier(field) for field in LIST_FIELDS),
            ),
            (feedback_id,),
        )
        row = cur.fetchone()
    if row is None:
        raise ResourceNotFoundError("progress feedback not found")
    return normalize_row(row)


def normalize_row(row: dict[str, Any]) -> dict[str, Any]:
    return {
        key: str(value) if key in {"id", "item_id", "assignee_id"} and value is not None
        else value.isoformat() if hasattr(value, "isoformat")
        else value
        for key, value in row.items()
    }
