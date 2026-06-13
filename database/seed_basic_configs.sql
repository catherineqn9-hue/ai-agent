INSERT INTO agent_config (
    id, agent_key, agent_name, agent_type, provider, system_prompt, tool_permissions, parameters, enabled
) VALUES
    (
        gen_random_uuid(),
        'task_parser',
        '任务解析 Agent',
        'task_parser',
        'langgraph',
        'Parse supervision Excel rows into structured supervision items.',
        '["parse_excel", "create_supervision_batch"]'::jsonb,
        '{"temperature": 0}'::jsonb,
        true
    ),
    (
        gen_random_uuid(),
        'owner_matcher',
        '责任人匹配 Agent',
        'owner_matcher',
        'langgraph',
        'Match responsible users through configured user-service APIs.',
        '["query_user_service", "create_item_assignee"]'::jsonb,
        '{"temperature": 0}'::jsonb,
        true
    ),
    (
        gen_random_uuid(),
        'supervision_assistant',
        'Supervision Assistant Agent',
        'assistant',
        'kimi',
        'Use configured business APIs to help users query, create, and follow up supervision items.',
        '["query_supervision_items", "create_supervision_item", "update_supervision_status", "progress_feedback", "query_progress_feedbacks", "query_import_batches", "query_import_batch_detail"]'::jsonb,
        '{"model":"moonshot-v1-8k","temperature":0.2}'::jsonb,
        true
    )
ON CONFLICT (agent_key) DO NOTHING;

INSERT INTO integration_config (
    id, integration_key, integration_name, integration_type, base_url, auth_type, auth_config, timeout_seconds, enabled
) VALUES
    (
        gen_random_uuid(),
        'user_service',
        '用户服务',
        'user_service',
        '',
        'none',
        '{}'::jsonb,
        10,
        false
    ),
    (
        gen_random_uuid(),
        'im_service',
        'IM 消息服务',
        'im_service',
        '',
        'none',
        '{}'::jsonb,
        10,
        false
    )
ON CONFLICT (integration_key) DO NOTHING;

INSERT INTO message_template (
    id, template_key, template_name, scene, channel, title_template, body_template, variables, enabled
) VALUES
    (
        gen_random_uuid(),
        'confirm_receive',
        '确认接收模板',
        'confirm_receive',
        'im',
        '请确认接收督办事项：{title}',
        '你有一条新的督办事项需要确认接收：{title}，截止时间：{deadline_at}。',
        '["title", "deadline_at"]'::jsonb,
        true
    ),
    (
        gen_random_uuid(),
        'due_reminder',
        '临期催办模板',
        'due_reminder',
        'im',
        '督办事项即将到期：{title}',
        '事项 {title} 即将到期，请反馈当前进度。',
        '["title"]'::jsonb,
        true
    )
ON CONFLICT (template_key) DO NOTHING;

INSERT INTO excel_field_mapping (
    id, mapping_key, mapping_name, source_column, target_field, required, transform_rule, enabled
) VALUES
    (
        gen_random_uuid(),
        'title',
        '任务标题',
        '关键任务',
        'title',
        true,
        '{}'::jsonb,
        true
    ),
    (
        gen_random_uuid(),
        'deadline_at',
        '截止时间',
        '预计完成时间',
        'deadline_at',
        true,
        '{"type": "date"}'::jsonb,
        true
    )
ON CONFLICT (mapping_key) DO NOTHING;

INSERT INTO reminder_rule (
    id, rule_key, rule_name, trigger_type, days_before_deadline, repeat_interval_hours, max_send_count, skip_holidays, enabled
) VALUES
    (
        gen_random_uuid(),
        'near_due_default',
        '默认临期催办',
        'near_due',
        1,
        24,
        3,
        true,
        true
    )
ON CONFLICT (rule_key) DO NOTHING;

INSERT INTO status_dict (
    id, status_key, status_name, status_group, sort_order, is_terminal, enabled
) VALUES
    (gen_random_uuid(), 'pending_assign', '待分派', 'supervision_item', 10, false, true),
    (gen_random_uuid(), 'pending_confirm', '待确认', 'supervision_item', 20, false, true),
    (gen_random_uuid(), 'in_progress', '进行中', 'supervision_item', 30, false, true),
    (gen_random_uuid(), 'near_due', '临期', 'supervision_item', 40, false, true),
    (gen_random_uuid(), 'completed', '已完成', 'supervision_item', 90, true, true),
    (gen_random_uuid(), 'overdue', '已逾期', 'supervision_item', 100, false, true)
ON CONFLICT (status_key) DO NOTHING;
