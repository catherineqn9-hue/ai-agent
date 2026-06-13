from typing import Any
from uuid import uuid4

from psycopg import Connection
from psycopg import sql
from psycopg.errors import UniqueViolation

from app.core.exceptions import DuplicateResourceError, ResourceNotFoundError


LIST_FIELDS = (
    "id",
    "batch_id",
    "item_no",
    "title",
    "description",
    "source_row_no",
    "priority",
    "status",
    "deadline_at",
    "completed_at",
    "created_by",
    "created_at",
    "updated_at",
)

CREATE_FIELDS = (
    "id",
    "batch_id",
    "item_no",
    "title",
    "description",
    "source_row_no",
    "priority",
    "status",
    "deadline_at",
    "created_by",
)

UPDATE_FIELDS = (
    "item_no",
    "title",
    "description",
    "priority",
    "status",
    "deadline_at",
    "completed_at",
    "created_by",
)


def list_items(conn: Connection) -> list[dict[str, Any]]:
    with conn.cursor() as cur:
        cur.execute(
            sql.SQL("SELECT {fields} FROM supervision_item ORDER BY updated_at DESC, created_at DESC").format(
                fields=sql.SQL(", ").join(sql.Identifier(field) for field in LIST_FIELDS),
            )
        )
        return [normalize_row(row) for row in cur.fetchall()]


def get_item(conn: Connection, item_id: str) -> dict[str, Any]:
    with conn.cursor() as cur:
        cur.execute(
            sql.SQL("SELECT {fields} FROM supervision_item WHERE id = %s").format(
                fields=sql.SQL(", ").join(sql.Identifier(field) for field in LIST_FIELDS),
            ),
            (item_id,),
        )
        row = cur.fetchone()
    if row is None:
        raise ResourceNotFoundError("supervision item not found")
    return normalize_row(row)


def create_item(conn: Connection, payload: dict[str, Any]) -> dict[str, Any]:
    values = {field: payload.get(field) for field in CREATE_FIELDS if field != "id"}
    values["id"] = str(uuid4())

    fields = tuple(values)
    params = tuple(values[field] for field in fields)
    try:
        with conn.cursor() as cur:
            cur.execute(
                sql.SQL("INSERT INTO supervision_item ({fields}) VALUES ({placeholders})").format(
                    fields=sql.SQL(", ").join(sql.Identifier(field) for field in fields),
                    placeholders=sql.SQL(", ").join(sql.Placeholder() for _ in fields),
                ),
                params,
            )
        conn.commit()
    except UniqueViolation as exc:
        conn.rollback()
        raise DuplicateResourceError("duplicate item no") from exc

    return get_item(conn, values["id"])


def update_item(conn: Connection, item_id: str, payload: dict[str, Any]) -> dict[str, Any]:
    values = {
        field: payload[field]
        for field in UPDATE_FIELDS
        if field in payload
    }
    if not values:
        return get_item(conn, item_id)

    assignments = [
        sql.SQL("{} = {}").format(sql.Identifier(field), sql.Placeholder())
        for field in values
    ]
    assignments.append(sql.SQL("updated_at = now()"))
    params = [values[field] for field in values]
    params.append(item_id)

    try:
        with conn.cursor() as cur:
            cur.execute(
                sql.SQL("UPDATE supervision_item SET {assignments} WHERE id = %s").format(
                    assignments=sql.SQL(", ").join(assignments),
                ),
                tuple(params),
            )
            if cur.rowcount == 0:
                conn.rollback()
                raise ResourceNotFoundError("supervision item not found")
        conn.commit()
    except UniqueViolation as exc:
        conn.rollback()
        raise DuplicateResourceError("duplicate item no") from exc

    return get_item(conn, item_id)


def delete_item(conn: Connection, item_id: str) -> None:
    with conn.cursor() as cur:
        cur.execute("DELETE FROM supervision_item WHERE id = %s", (item_id,))
        if cur.rowcount == 0:
            conn.rollback()
            raise ResourceNotFoundError("supervision item not found")
    conn.commit()


def normalize_row(row: dict[str, Any]) -> dict[str, Any]:
    return {
        key: str(value) if key in {"id", "batch_id"} and value is not None
        else value.isoformat() if hasattr(value, "isoformat")
        else value
        for key, value in row.items()
    }
