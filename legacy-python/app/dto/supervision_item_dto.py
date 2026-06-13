from datetime import datetime

from pydantic import BaseModel, Field


class SupervisionItemCreateRequest(BaseModel):
    item_no: str | None = None
    title: str = Field(..., min_length=1)
    description: str | None = None
    priority: str = "normal"
    status: str = "pending_assign"
    deadline_at: datetime | None = None
    created_by: str = "admin"


class SupervisionItemUpdateRequest(BaseModel):
    item_no: str | None = None
    title: str | None = None
    description: str | None = None
    priority: str | None = None
    status: str | None = None
    deadline_at: datetime | None = None
    completed_at: datetime | None = None
    created_by: str | None = None


class SupervisionItemStatusRequest(BaseModel):
    status: str = Field(..., min_length=1)


class SupervisionItemResponse(BaseModel):
    id: str
    batch_id: str | None = None
    item_no: str
    title: str
    description: str | None = None
    source_row_no: int | None = None
    priority: str
    status: str
    deadline_at: str | None = None
    completed_at: str | None = None
    created_by: str
    created_at: str | None = None
    updated_at: str | None = None
