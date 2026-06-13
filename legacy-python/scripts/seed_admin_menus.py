import json
import sys
from pathlib import Path
from uuid import uuid4

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from psycopg.types.json import Jsonb

from app.db import get_connection


MENUS = [
    {
        "menu_id": "assistant",
        "title": "AI 助手问答",
        "icon": "AI",
        "menu_type": "assistant",
        "resource": None,
        "hint": "通过对话调用后端接口能力",
        "group_name": "智能助手",
        "sort_order": 10,
        "table_fields": [],
        "form_fields": [],
    },
    {
        "menu_id": "agent",
        "title": "Agent 配置",
        "icon": "A",
        "menu_type": "crud",
        "resource": "agent-configs",
        "hint": "维护 Agent 开关、类型、Provider、提示词和可调用工具。",
        "group_name": "基础配置",
        "sort_order": 20,
        "table_fields": ["agent_key", "agent_name", "agent_type", "provider", "enabled"],
        "form_fields": [
            {"name": "agent_key", "label": "Agent Key", "required": True},
            {"name": "agent_name", "label": "Agent 名称", "required": True},
            {"name": "agent_type", "label": "Agent 类型", "required": True},
            {"name": "provider", "label": "Provider", "default": "langgraph"},
            {"name": "model_name", "label": "模型"},
            {"name": "system_prompt", "label": "系统提示词", "type": "textarea"},
            {"name": "tool_permissions", "label": "工具权限 JSON", "type": "json", "default": "[]"},
            {"name": "parameters", "label": "参数 JSON", "type": "json", "default": "{}"},
            {"name": "enabled", "label": "启用", "type": "boolean", "default": True},
        ],
    },
    {
        "menu_id": "supervision",
        "title": "督办事项",
        "icon": "D",
        "menu_type": "supervision",
        "resource": "supervision-items",
        "hint": "维护督办事项主数据，后续 Excel 导入和 AI 调用都会落到这里。",
        "group_name": "督办业务",
        "sort_order": 25,
        "table_fields": [],
        "form_fields": [],
    },
    {
        "menu_id": "integration",
        "title": "接口配置",
        "icon": "I",
        "menu_type": "crud",
        "resource": "integration-configs",
        "hint": "维护用户服务、IM 服务、OA 服务等外部接口配置。",
        "group_name": "基础配置",
        "sort_order": 30,
        "table_fields": ["integration_key", "integration_name", "integration_type", "base_url", "enabled"],
        "form_fields": [
            {"name": "integration_key", "label": "接口 Key", "required": True},
            {"name": "integration_name", "label": "接口名称", "required": True},
            {"name": "integration_type", "label": "接口类型", "required": True},
            {"name": "base_url", "label": "接口地址"},
            {"name": "auth_type", "label": "鉴权方式", "default": "none"},
            {"name": "auth_config", "label": "鉴权配置 JSON", "type": "json", "default": "{}"},
            {"name": "timeout_seconds", "label": "超时秒数", "type": "number", "default": 10},
            {"name": "enabled", "label": "启用", "type": "boolean", "default": False},
        ],
    },
    {
        "menu_id": "template",
        "title": "消息模板",
        "icon": "M",
        "menu_type": "crud",
        "resource": "message-templates",
        "hint": "维护确认接收、临期催办、逾期催办、进度反馈等消息模板。",
        "group_name": "基础配置",
        "sort_order": 40,
        "table_fields": ["template_key", "template_name", "scene", "channel", "enabled"],
        "form_fields": [
            {"name": "template_key", "label": "模板 Key", "required": True},
            {"name": "template_name", "label": "模板名称", "required": True},
            {"name": "scene", "label": "场景", "required": True},
            {"name": "channel", "label": "渠道", "default": "im"},
            {"name": "title_template", "label": "标题模板", "type": "textarea"},
            {"name": "body_template", "label": "正文模板", "type": "textarea"},
            {"name": "variables", "label": "变量 JSON", "type": "json", "default": "[]"},
            {"name": "enabled", "label": "启用", "type": "boolean", "default": True},
        ],
    },
    {
        "menu_id": "mapping",
        "title": "Excel 字段映射",
        "icon": "E",
        "menu_type": "crud",
        "resource": "excel-field-mappings",
        "hint": "维护 Excel 列名和督办事项字段的映射关系。",
        "group_name": "基础配置",
        "sort_order": 50,
        "table_fields": ["mapping_key", "mapping_name", "source_column", "target_field", "required", "enabled"],
        "form_fields": [
            {"name": "mapping_key", "label": "映射 Key", "required": True},
            {"name": "mapping_name", "label": "映射名称", "required": True},
            {"name": "source_column", "label": "Excel 列名", "required": True},
            {"name": "target_field", "label": "目标字段", "required": True},
            {"name": "required", "label": "必填", "type": "boolean", "default": False},
            {"name": "transform_rule", "label": "转换规则 JSON", "type": "json", "default": "{}"},
            {"name": "enabled", "label": "启用", "type": "boolean", "default": True},
        ],
    },
    {
        "menu_id": "excel_template",
        "title": "Excel 导入模板",
        "icon": "T",
        "menu_type": "crud",
        "resource": "excel-import-templates",
        "hint": "维护导入模板编码、策略编码和列映射配置。",
        "group_name": "基础配置",
        "sort_order": 55,
        "table_fields": ["template_code", "template_name", "handler_code", "source_columns", "entity_fields", "enabled"],
        "form_fields": [
            {"name": "template_code", "label": "模板 Code", "required": True},
            {"name": "template_name", "label": "模板名称", "required": True},
            {"name": "description", "label": "说明", "type": "textarea"},
            {"name": "handler_code", "label": "策略 Code", "default": "standard_supervision"},
            {
                "name": "source_columns",
                "label": "Excel 中文列 JSON",
                "type": "json",
                "default": "[\"事项编号\",\"事项标题\",\"描述\",\"优先级\",\"状态\",\"截止时间\",\"创建人\"]",
            },
            {
                "name": "entity_fields",
                "label": "实体英文字段 JSON",
                "type": "json",
                "default": "[\"item_no\",\"title\",\"description\",\"priority\",\"status\",\"deadline_at\",\"created_by\"]",
            },
            {
                "name": "mapping_config",
                "label": "扩展规则 JSON",
                "type": "json",
                "default": "{\"required_fields\":[\"title\"],\"aliases\":{}}",
            },
            {"name": "enabled", "label": "启用", "type": "boolean", "default": True},
        ],
    },
    {
        "menu_id": "reminder",
        "title": "催办规则",
        "icon": "R",
        "menu_type": "crud",
        "resource": "reminder-rules",
        "hint": "维护临期、逾期、重复提醒和跳过节假日策略。",
        "group_name": "基础配置",
        "sort_order": 60,
        "table_fields": ["rule_key", "rule_name", "trigger_type", "days_before_deadline", "enabled"],
        "form_fields": [
            {"name": "rule_key", "label": "规则 Key", "required": True},
            {"name": "rule_name", "label": "规则名称", "required": True},
            {"name": "trigger_type", "label": "触发类型", "required": True},
            {"name": "days_before_deadline", "label": "提前天数", "type": "number", "default": 1},
            {"name": "repeat_interval_hours", "label": "重复间隔小时", "type": "number", "default": 24},
            {"name": "max_send_count", "label": "最大发送次数", "type": "number", "default": 3},
            {"name": "skip_holidays", "label": "跳过节假日", "type": "boolean", "default": True},
            {"name": "enabled", "label": "启用", "type": "boolean", "default": True},
        ],
    },
    {
        "menu_id": "status",
        "title": "状态字典",
        "icon": "S",
        "menu_type": "crud",
        "resource": "status-dicts",
        "hint": "维护督办事项状态流转字典。",
        "group_name": "基础配置",
        "sort_order": 70,
        "table_fields": ["status_key", "status_name", "status_group", "sort_order", "is_terminal", "enabled"],
        "form_fields": [
            {"name": "status_key", "label": "状态 Key", "required": True},
            {"name": "status_name", "label": "状态名称", "required": True},
            {"name": "status_group", "label": "状态分组", "default": "supervision_item"},
            {"name": "sort_order", "label": "排序", "type": "number", "default": 0},
            {"name": "is_terminal", "label": "终态", "type": "boolean", "default": False},
            {"name": "enabled", "label": "启用", "type": "boolean", "default": True},
        ],
    },
    {
        "menu_id": "menu",
        "title": "菜单配置",
        "icon": "N",
        "menu_type": "crud",
        "resource": "admin-menus",
        "hint": "维护后台左侧菜单、绑定资源和表单字段。",
        "group_name": "基础配置",
        "sort_order": 80,
        "table_fields": ["menu_id", "title", "menu_type", "resource", "sort_order", "enabled"],
        "form_fields": [
            {"name": "menu_id", "label": "菜单 Key", "required": True},
            {"name": "title", "label": "菜单名称", "required": True},
            {"name": "icon", "label": "图标文本", "default": "N"},
            {"name": "menu_type", "label": "菜单类型", "default": "crud"},
            {"name": "resource", "label": "绑定资源"},
            {"name": "hint", "label": "说明", "type": "textarea"},
            {"name": "group_name", "label": "分组名称", "default": "基础配置"},
            {"name": "sort_order", "label": "排序", "type": "number", "default": 0},
            {"name": "table_fields", "label": "列表字段 JSON", "type": "json", "default": "[]"},
            {"name": "form_fields", "label": "表单字段 JSON", "type": "json", "default": "[]"},
            {"name": "enabled", "label": "启用", "type": "boolean", "default": True},
        ],
    },
]


DISABLED_MENU_IDS = {"mapping"}


def main() -> None:
    with get_connection() as conn:
        with conn.cursor() as cur:
            for menu in MENUS:
                cur.execute(
                    """
                    INSERT INTO admin_menu_config (
                        id,
                        menu_id,
                        title,
                        icon,
                        menu_type,
                        resource,
                        hint,
                        group_name,
                        sort_order,
                        table_fields,
                        form_fields,
                        enabled
                    )
                    VALUES (
                        %(id)s,
                        %(menu_id)s,
                        %(title)s,
                        %(icon)s,
                        %(menu_type)s,
                        %(resource)s,
                        %(hint)s,
                        %(group_name)s,
                        %(sort_order)s,
                        %(table_fields)s,
                        %(form_fields)s,
                        true
                    )
                    ON CONFLICT (menu_id) DO UPDATE SET
                        title = EXCLUDED.title,
                        icon = EXCLUDED.icon,
                        menu_type = EXCLUDED.menu_type,
                        resource = EXCLUDED.resource,
                        hint = EXCLUDED.hint,
                        group_name = EXCLUDED.group_name,
                        sort_order = EXCLUDED.sort_order,
                        table_fields = EXCLUDED.table_fields,
                        form_fields = EXCLUDED.form_fields,
                        enabled = EXCLUDED.enabled,
                        updated_at = now()
                    """,
                    {
                        **menu,
                        "id": str(uuid4()),
                        "table_fields": Jsonb(menu["table_fields"]),
                        "form_fields": Jsonb(menu["form_fields"]),
                    },
                )
            cur.execute(
                """
                UPDATE admin_menu_config
                SET enabled = false,
                    updated_at = now()
                WHERE menu_id = ANY(%s)
                """,
                (list(DISABLED_MENU_IDS),),
            )
        conn.commit()

    fallback = {
        "menus": [
            {
                "id": menu["menu_id"],
                "title": menu["title"],
                "icon": menu["icon"],
                "type": menu["menu_type"],
                "resource": menu["resource"],
                "hint": menu["hint"],
                "groupName": menu["group_name"],
                "tableFields": menu["table_fields"],
                "fields": menu["form_fields"],
            }
            for menu in MENUS
            if menu["menu_id"] not in DISABLED_MENU_IDS
        ]
    }
    Path("static/admin-menu.json").write_text(
        json.dumps(fallback, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )


if __name__ == "__main__":
    main()
