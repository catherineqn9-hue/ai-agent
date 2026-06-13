from typing import Any, TypedDict


class TraceItem(TypedDict):
    node: str
    detail: str


class AgentState(TypedDict, total=False):
    thread_id: str
    request_id: str
    user_id: str
    messages: list[dict[str, str]]
    intent: str | None
    slots: dict[str, str]
    missing_slots: list[str]
    need_clarification: bool
    questions: list[str]
    answer: str | None
    trace: list[TraceItem]
    status: str
    context: dict[str, Any]
