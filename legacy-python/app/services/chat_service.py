from uuid import uuid4

from app.agent.graph import agent_graph
from app.schemas import ChatRequest, ChatResponse
from app.storage import conversation_store


def run_agent_turn(request: ChatRequest) -> ChatResponse:
    thread_id = request.thread_id or str(uuid4())
    request_id = str(uuid4())
    trace_id = str(uuid4())

    previous_state = conversation_store.get(thread_id) or {}
    messages = [
        *previous_state.get("messages", []),
        {"role": "user", "content": request.message},
    ]

    initial_state = {
        **previous_state,
        "thread_id": thread_id,
        "request_id": request_id,
        "user_id": request.user_id,
        "messages": messages,
        "context": request.context,
        "trace": previous_state.get("trace", []),
    }

    result = agent_graph.invoke(
        initial_state,
        config={"configurable": {"thread_id": thread_id}},
    )
    result["trace_id"] = trace_id
    result["messages"] = [
        *result.get("messages", []),
        {"role": "assistant", "content": result.get("answer", "")},
    ]
    conversation_store.save(thread_id, result)

    return ChatResponse(
        request_id=request_id,
        thread_id=thread_id,
        answer=result.get("answer", ""),
        need_clarification=bool(result.get("need_clarification")),
        questions=result.get("questions", []),
        intent=result.get("intent"),
        slots=result.get("slots", {}),
        status=result.get("status", "completed"),
        trace_id=trace_id,
        trace=result.get("trace", []),
    )
