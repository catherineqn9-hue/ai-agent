from dataclasses import dataclass, field
from typing import Protocol

from app.agent.tools.supervision_tools import (
    add_progress_feedback_tool,
    create_supervision_item_tool,
    query_supervision_items_tool,
    update_supervision_status_tool,
)


@dataclass
class ActionResult:
    """Action 的统一返回结果。

    一期先把“追问”和“回复”做成 Action。
    二期接 OA 时，查询 OA、创建任务草稿、解析附件也按这个返回结构接入。
    这样 LangGraph 主流程不用重写，只需要新增 Action。
    """

    answer: str
    status: str
    questions: list[str] = field(default_factory=list)
    events: list[dict[str, str]] = field(default_factory=list)


class Action(Protocol):
    """所有业务动作的协议。

    领导能理解的说法：Action 就是“智能体能做的一件事”。
    技术落地的说法：Action 是可插拔函数，后续把 OA 能力挂进来。
    """

    name: str

    def run(self, state: dict) -> ActionResult:
        ...


class ClarifyAction:
    """一期已落地：信息不足时主动追问。

    对应资料里的 Slot Filling。
    例如用户只说“帮我跟进一下这个事情”，系统不会乱猜，而是追问：
    1. 具体事项是什么
    2. 涉及谁
    3. 什么时候完成
    """

    name = "clarify"

    def run(self, state: dict) -> ActionResult:
        questions = state.get("questions", [])
        answer = "为了把事项跟进清楚，请先补充：" + "；".join(questions)
        return ActionResult(
            answer=answer,
            questions=questions,
            status="waiting_user",
            events=[{"type": "clarification_requested", "count": str(len(questions))}],
        )


class ReplyAction:
    """一期已落地：信息足够时生成结构化回复。

    这里先用规则版输出，保证明天可以演示。
    后续可以把 answer 生成逻辑替换成 LLM 结构化输出。
    """

    name = "reply"

    def run(self, state: dict) -> ActionResult:
        slots = state.get("slots", {})
        task = slots.get("task", "当前事项")
        owner = slots.get("owner", "相关负责人")
        deadline = slots.get("deadline", "约定时间")
        goal = slots.get("goal", "完成推进并反馈结果")

        answer = "\n".join(
            [
                f"已整理为一条可跟进事项：{task}。",
                f"涉及对象：{owner}。",
                f"期望时间：{deadline}。",
                f"建议目标：{goal}。",
                "下一步建议：先确认责任人和当前进度，再形成待办记录；如果后续接入 OA，可将该内容转成任务草稿并等待人工确认后写入。",
            ]
        )
        return ActionResult(
            answer=answer,
            status="completed",
            events=[{"type": "reply_generated", "task": task}],
        )


class OaTaskDraftAction:
    """二期预留：创建 OA 任务草稿。

    这里故意不直接写 OA，因为一期目标是“能演示的对话闭环”。
    真接 OA 时，应该先生成草稿，等人工确认后再调用 OA 接口。
    这能避免 AI 误派单、重复派单、越权派单。
    """

    name = "oa_task_draft"

    def run(self, state: dict) -> ActionResult:
        slots = state.get("slots", {})
        return ActionResult(
            answer="已生成 OA 任务草稿。当前 MVP 不直接写入 OA，需二期接入 OA API 并增加人工确认。",
            status="draft_ready",
            events=[
                {
                    "type": "oa_task_draft_created",
                    "task": slots.get("task", ""),
                    "owner": slots.get("owner", ""),
                    "deadline": slots.get("deadline", ""),
                }
            ],
        )


class ParseAttachmentAction:
    """二期预留：附件解析。

    后续领导上传附件后，可以在这里解析 Word/PDF/Excel，
    抽取任务事项、责任人、时间要求，再回填到 slots。
    """

    name = "parse_attachment"

    def run(self, state: dict) -> ActionResult:
        return ActionResult(
            answer="附件解析能力待二期接入。当前 MVP 先通过文本对话补齐任务信息。",
            status="not_enabled",
            events=[{"type": "attachment_parse_skipped"}],
        )


class SupervisionToolAction:
    name = "supervision_tool"

    def run(self, state: dict) -> ActionResult:
        intent = state.get("intent")
        context = state.get("context", {})
        slots = state.get("slots", {})

        if intent == "query_supervision_items":
            result = query_supervision_items_tool(
                status=context.get("status"),
                keyword=context.get("keyword"),
            )
            items = result["items"][:5]
            if not items:
                answer = "当前没有查到匹配的督办事项。"
            else:
                lines = [f"查到 {result['count']} 条督办事项，先列前 {len(items)} 条："]
                lines.extend(
                    f"- {item['title']}（{item['status']}，编号 {item['item_no']}）"
                    for item in items
                )
                answer = "\n".join(lines)
            return ActionResult(
                answer=answer,
                status="completed",
                events=[{"type": "tool_called", "tool": "query_supervision_items"}],
            )

        if intent == "create_supervision_item":
            title = slots.get("task") or context.get("title")
            if not title:
                return ActionResult(
                    answer="请补充要创建的督办事项标题。",
                    status="waiting_user",
                    questions=["要创建的督办事项标题是什么？"],
                    events=[{"type": "clarification_requested", "count": "1"}],
                )
            item = create_supervision_item_tool(
                title=title,
                description=context.get("description"),
                priority=context.get("priority", "normal"),
                deadline_at=context.get("deadline_at"),
                created_by=str(state.get("user_id") or "ai_assistant"),
            )
            return ActionResult(
                answer=f"已创建督办事项：{item['title']}，编号 {item['item_no']}。",
                status="completed",
                events=[{"type": "tool_called", "tool": "create_supervision_item"}],
            )

        if intent == "update_supervision_status":
            item_id = context.get("item_id")
            status = context.get("status", "completed")
            if not item_id:
                return ActionResult(
                    answer="请先指定要更新状态的督办事项。",
                    status="waiting_user",
                    questions=["要更新哪条督办事项？"],
                    events=[{"type": "clarification_requested", "count": "1"}],
                )
            item = update_supervision_status_tool(item_id=item_id, status=status)
            return ActionResult(
                answer=f"已更新督办事项状态：{item['title']} -> {item['status']}。",
                status="completed",
                events=[{"type": "tool_called", "tool": "update_supervision_status"}],
            )

        if intent == "progress_feedback":
            item_id = context.get("item_id")
            if not item_id:
                return ActionResult(
                    answer="请先指定要反馈进度的督办事项。",
                    status="waiting_user",
                    questions=["要给哪条督办事项写进度反馈？"],
                    events=[{"type": "clarification_requested", "count": "1"}],
                )
            feedback = add_progress_feedback_tool(
                item_id=item_id,
                content=context.get("content") or state.get("messages", [{}])[-1].get("content", ""),
                progress_percent=int(context.get("progress_percent", 0)),
                risk_note=context.get("risk_note"),
                feedback_user_id=str(state.get("user_id") or "ai_assistant"),
                feedback_user_name=context.get("feedback_user_name", "AI 助手"),
            )
            return ActionResult(
                answer=f"已记录进度反馈：{feedback['progress_percent']}%，{feedback['content']}",
                status="completed",
                events=[{"type": "tool_called", "tool": "add_progress_feedback"}],
            )

        return ActionResult(
            answer="当前业务工具暂不支持这个操作。",
            status="not_supported",
            events=[{"type": "tool_skipped"}],
        )


ACTION_REGISTRY: dict[str, Action] = {
    # 一期真实可演示
    "clarify": ClarifyAction(),
    "reply": ReplyAction(),
    # 二期业务系统接入口
    "oa_task_draft": OaTaskDraftAction(),
    "parse_attachment": ParseAttachmentAction(),
    "supervision_tool": SupervisionToolAction(),
}


def run_action(name: str, state: dict) -> ActionResult:
    """执行 Action。

    这就是后续扩展的核心入口：
    - 一期：clarify / reply
    - 二期：oa_task_draft / parse_attachment / send_notification
    """

    action = ACTION_REGISTRY[name]
    return action.run(state)
