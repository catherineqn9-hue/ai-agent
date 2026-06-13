from fastapi import APIRouter, HTTPException

from app.schemas import ChatHistoryResponse, ChatRequest, ChatResponse, ChatStateResponse
from app.services.chat_service import run_agent_turn
from app.storage import conversation_store


router = APIRouter(prefix="/api/v1/chat", tags=["chat"])


@router.post("/run", response_model=ChatResponse)
def chat_run(request: ChatRequest) -> ChatResponse:
    return run_agent_turn(request)


@router.post("/resume", response_model=ChatResponse)
def chat_resume(request: ChatRequest) -> ChatResponse:
    if not request.thread_id:
        raise HTTPException(status_code=400, detail="resume requires thread_id")
    if conversation_store.get(request.thread_id) is None:
        raise HTTPException(status_code=404, detail="thread_id not found")
    return run_agent_turn(request)


@router.get("/state/{thread_id}", response_model=ChatStateResponse)
def chat_state(thread_id: str) -> ChatStateResponse:
    state = conversation_store.get(thread_id)
    if state is None:
        raise HTTPException(status_code=404, detail="thread_id not found")
    return ChatStateResponse(thread_id=thread_id, state=state)


@router.get("/history/{thread_id}", response_model=ChatHistoryResponse)
def chat_history(thread_id: str) -> ChatHistoryResponse:
    state = conversation_store.get(thread_id)
    if state is None:
        raise HTTPException(status_code=404, detail="thread_id not found")
    return ChatHistoryResponse(
        thread_id=thread_id,
        messages=conversation_store.messages(thread_id),
        trace=conversation_store.trace(thread_id),
    )
