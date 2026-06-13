from datetime import datetime
from typing import Any

from app.db import get_connection
from app.dto.supervision_item_dto import (
    SupervisionItemCreateRequest,
    SupervisionItemUpdateRequest,
)
from app.repositories.supervision_item_repository import (
    create_item,
    delete_item,
    get_item,
    list_items,
    update_item,
)


def list_supervision_items() -> list[dict[str, Any]]:
    with get_connection() as conn:
        return list_items(conn)


def get_supervision_item(item_id: str) -> dict[str, Any]:
    with get_connection() as conn:
        return get_item(conn, item_id)


def create_supervision_item(request: SupervisionItemCreateRequest) -> dict[str, Any]:
    payload = request.model_dump()
    payload["item_no"] = payload["item_no"] or generate_item_no()
    with get_connection() as conn:
        return create_item(conn, payload)


def update_supervision_item(
    item_id: str,
    request: SupervisionItemUpdateRequest,
) -> dict[str, Any]:
    payload = request.model_dump(exclude_unset=True)
    if payload.get("item_no") == "":
        payload.pop("item_no")
    with get_connection() as conn:
        return update_item(conn, item_id, payload)


def change_supervision_item_status(item_id: str, status: str) -> dict[str, Any]:
    payload: dict[str, Any] = {"status": status}
    if status == "completed":
        payload["completed_at"] = datetime.now().astimezone()
    with get_connection() as conn:
        return update_item(conn, item_id, payload)


def delete_supervision_item(item_id: str) -> None:
    with get_connection() as conn:
        delete_item(conn, item_id)


def generate_item_no() -> str:
    return f"ITEM-{datetime.now().strftime('%Y%m%d%H%M%S%f')}"
