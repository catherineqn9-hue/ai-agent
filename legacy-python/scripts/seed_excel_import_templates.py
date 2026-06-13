import sys
from pathlib import Path
from uuid import uuid4

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from psycopg.types.json import Jsonb

from app.db import get_connection


TEMPLATES = [
    {
        "template_code": "standard_supervision",
        "template_name": "标准督办事项模板",
        "description": "source_columns 存 Excel 中文列名，entity_fields 存实体英文字段，按顺序一一对应。",
        "handler_code": "standard_supervision",
        "source_columns": ["事项编号", "事项标题", "描述", "优先级", "状态", "截止时间", "创建人"],
        "entity_fields": [
            "item_no",
            "title",
            "description",
            "priority",
            "status",
            "deadline_at",
            "created_by",
        ],
        "mapping_config": {
            "required_fields": ["title"],
            "aliases": {
                "item_no": ["编号", "item_no", "Item No"],
                "title": ["标题", "任务标题", "title", "Title"],
                "description": ["任务描述", "说明", "description", "Description"],
                "priority": ["priority", "Priority"],
                "status": ["status", "Status"],
                "deadline_at": ["截止日期", "deadline_at", "Deadline"],
                "created_by": ["created_by", "Created By"],
            },
        },
    }
]


def main() -> None:
    with get_connection() as conn:
        with conn.cursor() as cur:
            for template in TEMPLATES:
                cur.execute(
                    """
                    INSERT INTO excel_import_template (
                        id,
                        template_code,
                        template_name,
                        description,
                        handler_code,
                        source_columns,
                        entity_fields,
                        mapping_config,
                        enabled
                    )
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, true)
                    ON CONFLICT (template_code) DO UPDATE SET
                        template_name = EXCLUDED.template_name,
                        description = EXCLUDED.description,
                        handler_code = EXCLUDED.handler_code,
                        source_columns = EXCLUDED.source_columns,
                        entity_fields = EXCLUDED.entity_fields,
                        mapping_config = EXCLUDED.mapping_config,
                        enabled = EXCLUDED.enabled,
                        updated_at = now()
                    """,
                    (
                        str(uuid4()),
                        template["template_code"],
                        template["template_name"],
                        template["description"],
                        template["handler_code"],
                        Jsonb(template["source_columns"]),
                        Jsonb(template["entity_fields"]),
                        Jsonb(template["mapping_config"]),
                    ),
                )
        conn.commit()


if __name__ == "__main__":
    main()
