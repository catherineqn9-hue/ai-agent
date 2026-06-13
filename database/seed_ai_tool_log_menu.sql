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
) VALUES (
    gen_random_uuid(),
    'ai_tool_logs',
    'AI 调用日志',
    'L',
    'crud',
    'ai-tool-call-logs',
    '查看 AI 通过业务接口办事的调用记录、入参、出参、状态和耗时。',
    '智能助手',
    20,
    '["created_at", "tool_name", "status", "duration_ms", "error_message", "request_id"]'::jsonb,
    '[
        {"name":"created_at","label":"调用时间"},
        {"name":"request_id","label":"请求 ID"},
        {"name":"thread_id","label":"会话 ID"},
        {"name":"agent_key","label":"Agent"},
        {"name":"tool_name","label":"工具名称"},
        {"name":"input_payload","label":"入参","type":"json"},
        {"name":"output_payload","label":"出参","type":"json"},
        {"name":"status","label":"状态"},
        {"name":"error_message","label":"错误信息"},
        {"name":"duration_ms","label":"耗时(ms)"}
    ]'::jsonb,
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
    updated_at = now();

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
) VALUES (
    gen_random_uuid(),
    'my_supervision',
    '我的督办台',
    '我',
    'my-supervision',
    'my-supervision-items',
    '查看分配给当前登录用户的督办事项、分派来源、接收状态和进度反馈。',
    '督办管理',
    25,
    '[]'::jsonb,
    '[]'::jsonb,
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
    updated_at = now();

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
) VALUES (
    gen_random_uuid(),
    'user_management',
    '用户管理',
    'U',
    'user-management',
    'users',
    '维护用户所属部门和分配角色，供督办智能分配使用。',
    '组织权限',
    35,
    '[]'::jsonb,
    '[]'::jsonb,
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
    updated_at = now();

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
) VALUES (
    gen_random_uuid(),
    'agent_configs',
    'Agent 配置',
    'A',
    'crud',
    'agent-configs',
    '管理 Agent、权限范围和允许工具。',
    '基础配置',
    40,
    '["agent_key", "agent_name", "provider", "enabled", "updated_at"]'::jsonb,
    '[
        {"name":"agent_key","label":"Agent Key"},
        {"name":"agent_name","label":"Agent 名称"},
        {"name":"agent_type","label":"Agent 类型"},
        {"name":"provider","label":"Provider"},
        {"name":"model_name","label":"模型"},
        {"name":"system_prompt","label":"系统提示词","type":"textarea"},
        {"name":"tool_permissions","label":"工具权限","type":"json"},
        {"name":"parameters","label":"参数","type":"json"},
        {"name":"enabled","label":"启用","type":"checkbox"}
    ]'::jsonb,
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
    updated_at = now();

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
) VALUES (
    gen_random_uuid(),
    'excel_templates',
    'Excel 模板',
    'X',
    'crud',
    'excel-import-templates',
    '维护 Excel 导入字段映射。',
    '基础配置',
    50,
    '["template_code", "template_name", "handler_code", "enabled"]'::jsonb,
    '[
        {"name":"template_code","label":"模板编码"},
        {"name":"template_name","label":"模板名称"},
        {"name":"description","label":"描述","type":"textarea"},
        {"name":"handler_code","label":"处理器编码"},
        {"name":"source_columns","label":"源列名","type":"json"},
        {"name":"entity_fields","label":"实体字段","type":"json"},
        {"name":"mapping_config","label":"映射配置","type":"json"},
        {"name":"enabled","label":"启用","type":"checkbox"}
    ]'::jsonb,
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
    updated_at = now();
