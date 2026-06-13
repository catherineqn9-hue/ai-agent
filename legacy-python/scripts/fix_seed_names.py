import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from app.db import get_connection


UPDATES = [
    ("UPDATE agent_config SET agent_name=%s WHERE agent_key=%s", ("任务解析 Agent", "task_parser")),
    ("UPDATE agent_config SET agent_name=%s WHERE agent_key=%s", ("责任人匹配 Agent", "owner_matcher")),
    ("UPDATE integration_config SET integration_name=%s WHERE integration_key=%s", ("用户服务", "user_service")),
    ("UPDATE integration_config SET integration_name=%s WHERE integration_key=%s", ("IM 消息服务", "im_service")),
    (
        "UPDATE excel_field_mapping SET mapping_name=%s, source_column=%s WHERE mapping_key=%s",
        ("任务标题", "关键任务", "title"),
    ),
    (
        "UPDATE excel_field_mapping SET mapping_name=%s, source_column=%s WHERE mapping_key=%s",
        ("截止时间", "预计完成时间", "deadline_at"),
    ),
    ("UPDATE reminder_rule SET rule_name=%s WHERE rule_key=%s", ("默认临期催办", "near_due_default")),
]


def main() -> None:
    with get_connection() as conn:
        with conn.cursor() as cur:
            for statement, params in UPDATES:
                cur.execute(statement, params)
        conn.commit()


if __name__ == "__main__":
    main()
