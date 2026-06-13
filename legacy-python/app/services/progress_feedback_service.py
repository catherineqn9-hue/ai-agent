from typing import Any

from app.db import get_connection
from app.dto.progress_feedback_dto import ProgressFeedbackCreateRequest
from app.repositories.progress_feedback_repository import create_feedback, list_feedbacks


def list_progress_feedbacks(item_id: str | None = None) -> list[dict[str, Any]]:
    with get_connection() as conn:
        return list_feedbacks(conn, item_id)


def create_progress_feedback(request: ProgressFeedbackCreateRequest) -> dict[str, Any]:
    with get_connection() as conn:
        return create_feedback(conn, request.model_dump())
