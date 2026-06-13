from langgraph.checkpoint.memory import MemorySaver
from langgraph.graph import END, StateGraph

from app.agent.actions import run_action
from app.agent.rules import (
    build_questions,
    detect_intent,
    extract_slots,
    find_missing_slots,
)
from app.agent.state import AgentState


def add_trace(state: AgentState, node: str, detail: str) -> list[dict[str, str]]:
    return [*state.get("trace", []), {"node": node, "detail": detail}]


def normalize_input(state: AgentState) -> AgentState:
    # 落地点 1：Context 构建。
    # 把当前输入、历史消息、用户信息放进 AgentState，后续每个节点都基于这个状态继续处理。
    messages = state.get("messages", [])
    return {
        **state,
        "messages": messages,
        "slots": state.get("slots", {}),
        "trace": add_trace(state, "normalize_input", "标准化输入和历史上下文"),
    }


def understand_intent(state: AgentState) -> AgentState:
    # 落地点 2：意图理解。
    # MVP 先用规则识别，后续可以替换成 LLM 结构化输出，不影响后面的 Flow。
    message = state["messages"][-1]["content"] if state.get("messages") else ""
    intent = detect_intent(message)
    return {
        **state,
        "intent": intent,
        "trace": add_trace(state, "understand_intent", f"识别意图：{intent}"),
    }


def extract_task_slots(state: AgentState) -> AgentState:
    # 落地点 3：Slot Filling。
    # 抽取任务事项、涉及对象、完成时间等槽位；缺什么，后面就追问什么。
    message = state["messages"][-1]["content"] if state.get("messages") else ""
    slots = extract_slots(message, state.get("slots", {}))
    return {
        **state,
        "slots": slots,
        "trace": add_trace(state, "extract_slots", f"当前槽位：{slots}"),
    }


def judge_completeness(state: AgentState) -> AgentState:
    # 落地点 4：流程分支。
    # 信息不足走澄清 Action，信息足够走回复 Action。
    # 这是任务型对话和普通聊天最大的区别：不是乱答，而是先判断能不能答。
    missing_slots = find_missing_slots(state.get("slots", {}))
    return {
        **state,
        "missing_slots": missing_slots,
        "need_clarification": bool(missing_slots),
        "trace": add_trace(state, "judge_completeness", f"缺失槽位：{missing_slots}"),
    }


def ask_clarification(state: AgentState) -> AgentState:
    questions = build_questions(state.get("missing_slots", []))
    action_result = run_action("clarify", {**state, "questions": questions})
    return {
        **state,
        "questions": action_result.questions,
        "answer": action_result.answer,
        "status": action_result.status,
        "events": [*state.get("events", []), *action_result.events],
        "trace": add_trace(state, "ask_clarification", "信息不足，生成澄清问题"),
    }


def generate_answer(state: AgentState) -> AgentState:
    # 落地点 5：Action 执行。
    # 一期执行 reply Action；二期这里可以按条件切到 oa_task_draft、parse_attachment 等业务动作。
    action_name = (
        "supervision_tool"
        if state.get("intent")
        in {
            "query_supervision_items",
            "create_supervision_item",
            "update_supervision_status",
            "progress_feedback",
        }
        else "reply"
    )
    action_result = run_action(action_name, state)
    return {
        **state,
        "questions": [],
        "need_clarification": False,
        "answer": action_result.answer,
        "status": action_result.status,
        "events": [*state.get("events", []), *action_result.events],
        "trace": add_trace(state, "generate_answer", "信息足够，生成结构化回复"),
    }


def validate_output(state: AgentState) -> AgentState:
    if not state.get("answer"):
        state["answer"] = "当前信息不足，请补充要跟进的事项、涉及对象和期望完成时间。"
        state["need_clarification"] = True
    return {
        **state,
        "trace": add_trace(state, "validate_output", "校验并补齐输出格式"),
    }


def route_after_judge(state: AgentState) -> str:
    if state.get("intent") == "query_supervision_items":
        return "generate_answer"
    if state.get("intent") in {"update_supervision_status", "progress_feedback"} and state.get(
        "context", {}
    ).get("item_id"):
        return "generate_answer"
    return "ask_clarification" if state.get("need_clarification") else "generate_answer"


def build_graph():
    # LangGraph 把“能落地的流程”写成状态图。
    # 领导汇报时可以这样解释：
    # 用户输入 -> 理解 -> 抽槽 -> 判断 -> 追问/回复 -> 校验 -> 返回。
    workflow = StateGraph(AgentState)
    workflow.add_node("normalize_input", normalize_input)
    workflow.add_node("understand_intent", understand_intent)
    workflow.add_node("extract_slots", extract_task_slots)
    workflow.add_node("judge_completeness", judge_completeness)
    workflow.add_node("ask_clarification", ask_clarification)
    workflow.add_node("generate_answer", generate_answer)
    workflow.add_node("validate_output", validate_output)

    workflow.set_entry_point("normalize_input")
    workflow.add_edge("normalize_input", "understand_intent")
    workflow.add_edge("understand_intent", "extract_slots")
    workflow.add_edge("extract_slots", "judge_completeness")
    workflow.add_conditional_edges(
        "judge_completeness",
        route_after_judge,
        {
            "ask_clarification": "ask_clarification",
            "generate_answer": "generate_answer",
        },
    )
    workflow.add_edge("ask_clarification", "validate_output")
    workflow.add_edge("generate_answer", "validate_output")
    workflow.add_edge("validate_output", END)
    return workflow.compile(checkpointer=MemorySaver())


agent_graph = build_graph()
