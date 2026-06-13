from typing import Any
from uuid import uuid4

from psycopg import Connection


def create_batch(
    conn: Connection,
    *,
    batch_no: str,
    batch_name: str,
    source_file_id: str | None,
    created_by: str,
) -> dict[str, Any]:
    batch_id = str(uuid4())
    with conn.cursor() as cur:
        cur.execute(
            """
            INSERT INTO supervision_batch (
                id,
                batch_no,
                batch_name,
                source_file_id,
                import_status,
                created_by
            )
            VALUES (%s, %s, %s, %s, 'parsing', %s)
            RETURNING id, batch_no, batch_name, import_status, total_count, success_count, failed_count
            """,
            (batch_id, batch_no, batch_name, source_file_id, created_by),
        )
        row = cur.fetchone()
    conn.commit()
    return normalize_row(row)


def finish_batch(
    conn: Connection,
    batch_id: str,
    *,
    import_status: str,
    total_count: int,
    success_count: int,
    failed_count: int,
) -> dict[str, Any]:
    with conn.cursor() as cur:
        cur.execute(
            """
            UPDATE supervision_batch
            SET import_status = %s,
                total_count = %s,
                success_count = %s,
                failed_count = %s,
                updated_at = now()
            WHERE id = %s
            RETURNING id, batch_no, batch_name, import_status, total_count, success_count, failed_count
            """,
            (import_status, total_count, success_count, failed_count, batch_id),
        )
        row = cur.fetchone()
    conn.commit()
    return normalize_row(row)


def normalize_row(row: dict[str, Any]) -> dict[str, Any]:
    return {
        key: str(value) if key == "id" and value is not None
        else value.isoformat() if hasattr(value, "isoformat")
        else value
        for key, value in row.items()
    }
