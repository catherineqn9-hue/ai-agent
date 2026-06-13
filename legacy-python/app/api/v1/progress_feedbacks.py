from fastapi import APIRouter, HTTPException

from app.dto.progress_feedback_dto import (
    ProgressFeedbackCreateRequest,
    ProgressFeedbackResponse,
)
from app.services.progress_feedback_service import (
    create_progress_feedback,
    list_progress_feedbacks,
)


router = APIRouter(prefix="/api/v1/progress-feedbacks", tags=["progress-feedbacks"])


@router.get("")
def list_feedbacks(item_id: str | None = None) -> dict[str, list[ProgressFeedbackResponse]]:
    return {"items": list_progress_feedbacks(item_id)}


@router.post("", response_model=ProgressFeedbackResponse)
def create_feedback(request: ProgressFeedbackCreateRequest) -> dict:
    try:
        return create_progress_feedback(request)
    except Exception as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
