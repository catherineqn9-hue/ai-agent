from typing import Any

from app.dto.progress_feedback_dto import ProgressFeedbackCreateRequest
from app.dto.supervision_item_dto import (
    SupervisionItemCreateRequest,
    SupervisionItemStatusRequest,
)
from app.services.progress_feedback_service import create_progress_feedback
from app.services.supervision_item_service import (
    change_supervision_item_status,
    create_supervision_item,
    list_supervision_items,
)


def query_supervision_items_tool(status: str | None = None, keyword: str | None = None) -> dict[str, Any]:
    items = list_supervision_items()
    if status:
        items = [item for item in items if item["status"] == status]
    if keyword:
        items = [
            item
            for item in items
            if keyword in item["title"] or keyword in (item.get("description") or "")
        ]
    return {"items": items, "count": len(items)}


def create_supervision_item_tool(
    *,
    title: str,
    description: str | None = None,
    priority: str = "normal",
    deadline_at: str | None = None,
    created_by: str = "ai_assistant",
) -> dict[str, Any]:
    request = SupervisionItemCreateRequest(
        title=title,
        description=description,
        priority=priority,
        deadline_at=deadline_at,
        created_by=created_by,
    )
    return create_supervision_item(request)


def update_supervision_status_tool(item_id: str, status: str) -> dict[str, Any]:
    request = SupervisionItemStatusRequest(status=status)
    return change_supervision_item_status(item_id, request.status)


def add_progress_feedback_tool(
    *,
    item_id: str,
    content: str,
    progress_percent: int = 0,
    risk_note: str | None = None,
    feedback_user_id: str = "ai_assistant",
    feedback_user_name: str = "AI 助手",
) -> dict[str, Any]:
    request = ProgressFeedbackCreateRequest(
        item_id=item_id,
        feedback_user_id=feedback_user_id,
        feedback_user_name=feedback_user_name,
        progress_percent=progress_percent,
        content=content,
        risk_note=risk_note,
    )
    return create_progress_feedback(request)
