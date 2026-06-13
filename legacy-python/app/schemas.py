from typing import Any

from pydantic import BaseModel, Field


class ChatRequest(BaseModel):
    user_id: str = Field(..., description="用户 ID")
    message: str = Field(..., min_length=1, description="用户本轮输入")
    thread_id: str | None = Field(default=None, description="会话 ID，不传则新建")
    context: dict[str, Any] = Field(default_factory=dict, description="业务上下文")


class ChatResponse(BaseModel):
    request_id: str
    thread_id: str
    answer: str
    need_clarification: bool
    questions: list[str] = Field(default_factory=list)
    intent: str | None = None
    slots: dict[str, str] = Field(default_factory=dict)
    status: str
    trace_id: str
    trace: list[dict[str, str]] = Field(default_factory=list)


class ChatStateResponse(BaseModel):
    thread_id: str
    state: dict[str, Any]


class ChatHistoryResponse(BaseModel):
    thread_id: str
    messages: list[dict[str, str]]
    trace: list[dict[str, str]]


class BusinessConfigCreate(BaseModel):
    config_type: str = Field(..., min_length=1, description="Business config type")
    key: str = Field(..., min_length=1, description="Unique key within config type")
    name: str = Field(..., min_length=1, description="Display name")
    description: str = ""
    enabled: bool = True
    data: dict[str, Any] = Field(default_factory=dict)


class BusinessConfigUpdate(BaseModel):
    key: str | None = None
    name: str | None = None
    description: str | None = None
    enabled: bool | None = None
    data: dict[str, Any] | None = None


class BusinessConfigItem(BusinessConfigCreate):
    id: str


class BusinessConfigListResponse(BaseModel):
    items: list[BusinessConfigItem]
