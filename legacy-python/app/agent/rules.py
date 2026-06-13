import re


REQUIRED_SLOTS = ["task", "owner", "deadline"]


def detect_intent(message: str) -> str:
    text = message.strip()
    if any(word in text for word in ["查询督办", "查看督办", "有哪些督办", "督办事项列表"]):
        return "query_supervision_items"
    if any(word in text for word in ["新增督办", "创建督办", "新增事项", "创建事项"]):
        return "create_supervision_item"
    if any(word in text for word in ["标记完成", "设为完成", "更新状态", "改成完成"]):
        return "update_supervision_status"
    if any(word in text for word in ["进度反馈", "反馈进度", "记录进度"]):
        return "progress_feedback"
    if any(word in text for word in ["确认", "可以", "收到", "补充", "是关于"]):
        return "provide_info"
    if any(word in text for word in ["进度", "反馈", "完成了", "已完成"]):
        return "progress_feedback"
    if any(word in text for word in ["跟进", "督办", "安排", "处理", "迁移", "任务"]):
        return "task_followup"
    if any(word in text for word in ["政策", "怎么", "如何", "是什么"]):
        return "question_answer"
    return "task_followup"


def extract_slots(message: str, current_slots: dict[str, str] | None = None) -> dict[str, str]:
    slots = dict(current_slots or {})
    text = normalize_text(message)

    task = extract_task(text)
    if task:
        slots["task"] = task

    owner = extract_owner(text)
    if owner:
        slots["owner"] = owner

    deadline = extract_deadline(text)
    if deadline:
        slots["deadline"] = deadline

    goal = extract_goal(text)
    if goal:
        slots["goal"] = goal

    return slots


def find_missing_slots(slots: dict[str, str]) -> list[str]:
    return [name for name in REQUIRED_SLOTS if not slots.get(name)]


def build_questions(missing_slots: list[str]) -> list[str]:
    mapping = {
        "task": "具体要跟进的事项是什么？",
        "owner": "这个事项涉及哪个部门或负责人？",
        "deadline": "期望什么时候完成或反馈？",
    }
    return [mapping[name] for name in missing_slots if name in mapping][:3]


def build_answer(slots: dict[str, str], intent: str | None) -> str:
    task = slots.get("task", "当前事项")
    owner = slots.get("owner", "相关负责人")
    deadline = slots.get("deadline", "约定时间")
    goal = slots.get("goal", "完成推进并反馈结果")

    lines = [
        f"已整理为一条可跟进事项：{task}。",
        f"涉及对象：{owner}。",
        f"期望时间：{deadline}。",
        f"建议目标：{goal}。",
        "下一步建议：先确认责任人和当前进度，再形成待办记录；如果后续接入 OA，可将该内容转成任务草稿并等待人工确认后写入。",
    ]
    if intent == "progress_feedback":
        lines.append("当前识别为进度反馈场景，建议补充完成比例、风险点和下一步动作。")
    return "\n".join(lines)


def normalize_text(message: str) -> str:
    return re.sub(r"\s+", "", message.strip())


def extract_task(text: str) -> str | None:
    patterns = [
        r"关于(.+?)的",
        r"跟进一下(.+?)(?:，|。|$)",
        r"跟进(.+?)(?:，|。|$)",
        r"处理(.+?)(?:，|。|$)",
        r"整理(.+?)(?:，|。|$)",
    ]
    for pattern in patterns:
        match = re.search(pattern, text)
        if match:
            value = cleanup(match.group(1))
            if value and value not in {"这个事情", "这个事", "事情", "一下"}:
                return value

    if "OA权限迁移" in text or "oa权限迁移" in text.lower():
        return "OA 权限迁移"
    if "权限迁移" in text:
        return "权限迁移"
    return None


def extract_owner(text: str) -> str | None:
    patterns = [
        r"涉及([^，。；;]+)",
        r"负责人[:：]?([^，。；;]+)",
        r"责任人[:：]?([^，。；;]+)",
    ]
    for pattern in patterns:
        match = re.search(pattern, text)
        if match:
            value = cleanup(match.group(1))
            if value:
                return value

    dept_match = re.search(r"([\u4e00-\u9fa5A-Za-z0-9]+部)", text)
    if dept_match:
        return dept_match.group(1)

    name_match = re.search(r"(张三|李四|王五|赵六)", text)
    if name_match:
        return name_match.group(1)
    return None


def extract_deadline(text: str) -> str | None:
    candidates = ["今天", "明天", "本周", "下周", "月底", "月末", "本月底", "下月底"]
    for item in candidates:
        if item in text:
            return item

    match = re.search(r"(\d{1,2}月\d{1,2}日|\d{1,2}号|\d{4}-\d{1,2}-\d{1,2})", text)
    if match:
        return match.group(1)

    if "月底前" in text:
        return "月底前"
    return None


def extract_goal(text: str) -> str | None:
    if "完成" in text:
        return "按期完成并反馈结果"
    if "反馈" in text:
        return "收集进度反馈并形成结论"
    if "整理" in text:
        return "整理信息并给出下一步建议"
    if "跟进" in text:
        return "推进事项并跟踪进度"
    return None


def cleanup(value: str) -> str:
    cleaned = value.strip("，。；;：: 的")
    cleaned = re.sub(r"^(一下|这个|这件)", "", cleaned)
    return cleaned.strip("，。；;：: 的").replace("OA", "OA ")
