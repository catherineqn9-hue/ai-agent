from pydantic import BaseModel, Field


class ProgressFeedbackCreateRequest(BaseModel):
    item_id: str = Field(..., min_length=1)
    feedback_user_id: str = "ai_assistant"
    feedback_user_name: str = "AI 助手"
    progress_percent: int = Field(default=0, ge=0, le=100)
    content: str = Field(..., min_length=1)
    risk_note: str | None = None
    attachment_ids: list[str] = Field(default_factory=list)


class ProgressFeedbackResponse(BaseModel):
    id: str
    item_id: str
    assignee_id: str | None = None
    feedback_user_id: str
    feedback_user_name: str
    progress_percent: int
    content: str
    risk_note: str | None = None
    attachment_ids: list[str]
    feedback_at: str | None = None
    created_at: str | None = None
