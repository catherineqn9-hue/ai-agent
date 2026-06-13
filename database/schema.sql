-- Sherry supervision service baseline schema.
-- Target database: PostgreSQL 14+.

CREATE TABLE IF NOT EXISTS agent_config (
    id UUID PRIMARY KEY,
    agent_key VARCHAR(80) NOT NULL UNIQUE,
    agent_name VARCHAR(120) NOT NULL,
    agent_type VARCHAR(60) NOT NULL,
    provider VARCHAR(60) NOT NULL DEFAULT 'langgraph',
    model_name VARCHAR(120),
    system_prompt TEXT,
    tool_permissions JSONB NOT NULL DEFAULT '[]'::jsonb,
    parameters JSONB NOT NULL DEFAULT '{}'::jsonb,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS integration_config (
    id UUID PRIMARY KEY,
    integration_key VARCHAR(80) NOT NULL UNIQUE,
    integration_name VARCHAR(120) NOT NULL,
    integration_type VARCHAR(60) NOT NULL,
    base_url VARCHAR(500),
    auth_type VARCHAR(40) NOT NULL DEFAULT 'none',
    auth_config JSONB NOT NULL DEFAULT '{}'::jsonb,
    timeout_seconds INTEGER NOT NULL DEFAULT 10,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS message_template (
    id UUID PRIMARY KEY,
    template_key VARCHAR(80) NOT NULL UNIQUE,
    template_name VARCHAR(120) NOT NULL,
    scene VARCHAR(60) NOT NULL,
    channel VARCHAR(40) NOT NULL DEFAULT 'im',
    title_template VARCHAR(300),
    body_template TEXT NOT NULL,
    variables JSONB NOT NULL DEFAULT '[]'::jsonb,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS excel_field_mapping (
    id UUID PRIMARY KEY,
    mapping_key VARCHAR(80) NOT NULL UNIQUE,
    mapping_name VARCHAR(120) NOT NULL,
    source_column VARCHAR(120) NOT NULL,
    target_field VARCHAR(120) NOT NULL,
    required BOOLEAN NOT NULL DEFAULT FALSE,
    transform_rule JSONB NOT NULL DEFAULT '{}'::jsonb,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS excel_import_template (
    id UUID PRIMARY KEY,
    template_code VARCHAR(80) NOT NULL UNIQUE,
    template_name VARCHAR(120) NOT NULL,
    description TEXT,
    handler_code VARCHAR(80) NOT NULL,
    source_columns JSONB NOT NULL DEFAULT '[]'::jsonb,
    entity_fields JSONB NOT NULL DEFAULT '[]'::jsonb,
    mapping_config JSONB NOT NULL DEFAULT '{}'::jsonb,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS reminder_rule (
    id UUID PRIMARY KEY,
    rule_key VARCHAR(80) NOT NULL UNIQUE,
    rule_name VARCHAR(120) NOT NULL,
    trigger_type VARCHAR(40) NOT NULL,
    days_before_deadline INTEGER NOT NULL DEFAULT 1,
    repeat_interval_hours INTEGER NOT NULL DEFAULT 24,
    max_send_count INTEGER NOT NULL DEFAULT 3,
    skip_holidays BOOLEAN NOT NULL DEFAULT TRUE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS status_dict (
    id UUID PRIMARY KEY,
    status_key VARCHAR(80) NOT NULL UNIQUE,
    status_name VARCHAR(120) NOT NULL,
    status_group VARCHAR(60) NOT NULL DEFAULT 'supervision_item',
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_terminal BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS app_user (
    id UUID PRIMARY KEY,
    username VARCHAR(80) NOT NULL UNIQUE,
    display_name VARCHAR(120) NOT NULL,
    department_id VARCHAR(80) NOT NULL DEFAULT 'operations',
    department_name VARCHAR(120) NOT NULL DEFAULT '运营部',
    role_key VARCHAR(60) NOT NULL DEFAULT 'member',
    role_name VARCHAR(120) NOT NULL DEFAULT '成员',
    password_hash VARCHAR(256) NOT NULL,
    password_salt VARCHAR(128) NOT NULL,
    session_token_hash VARCHAR(256),
    session_expires_at TIMESTAMPTZ,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS admin_menu_config (
    id UUID PRIMARY KEY,
    menu_id VARCHAR(80) NOT NULL UNIQUE,
    title VARCHAR(120) NOT NULL,
    icon VARCHAR(20) NOT NULL,
    menu_type VARCHAR(40) NOT NULL,
    resource VARCHAR(80),
    hint TEXT,
    group_name VARCHAR(80),
    sort_order INTEGER NOT NULL DEFAULT 0,
    table_fields JSONB NOT NULL DEFAULT '[]'::jsonb,
    form_fields JSONB NOT NULL DEFAULT '[]'::jsonb,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS supervision_batch (
    id UUID PRIMARY KEY,
    batch_no VARCHAR(80) NOT NULL UNIQUE,
    batch_name VARCHAR(200) NOT NULL,
    source_type VARCHAR(40) NOT NULL DEFAULT 'excel',
    source_file_id VARCHAR(120),
    import_status VARCHAR(40) NOT NULL DEFAULT 'pending',
    total_count INTEGER NOT NULL DEFAULT 0,
    success_count INTEGER NOT NULL DEFAULT 0,
    failed_count INTEGER NOT NULL DEFAULT 0,
    created_by VARCHAR(80) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS supervision_import_error (
    id UUID PRIMARY KEY,
    batch_id UUID NOT NULL REFERENCES supervision_batch(id) ON DELETE CASCADE,
    row_no INTEGER,
    item_no VARCHAR(80),
    title VARCHAR(300),
    error_message TEXT NOT NULL,
    raw_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS supervision_item (
    id UUID PRIMARY KEY,
    batch_id UUID REFERENCES supervision_batch(id),
    item_no VARCHAR(80) NOT NULL UNIQUE,
    title VARCHAR(300) NOT NULL,
    description TEXT,
    source_row_no INTEGER,
    priority VARCHAR(30) NOT NULL DEFAULT 'normal',
    status VARCHAR(40) NOT NULL DEFAULT 'pending_assign',
    deadline_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_by VARCHAR(80) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS item_assignee (
    id UUID PRIMARY KEY,
    item_id UUID NOT NULL REFERENCES supervision_item(id) ON DELETE CASCADE,
    assigned_by_user_id VARCHAR(80) NOT NULL DEFAULT 'system',
    assigned_by_name VARCHAR(120) NOT NULL DEFAULT '系统',
    assignee_user_id VARCHAR(80) NOT NULL,
    assignee_name VARCHAR(120) NOT NULL,
    department_id VARCHAR(80),
    department_name VARCHAR(120),
    role_type VARCHAR(40) NOT NULL DEFAULT 'owner',
    confirm_status VARCHAR(40) NOT NULL DEFAULT 'pending',
    assignment_note TEXT,
    rejection_reason TEXT,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    confirmed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS progress_feedback (
    id UUID PRIMARY KEY,
    item_id UUID NOT NULL REFERENCES supervision_item(id) ON DELETE CASCADE,
    assignee_id UUID REFERENCES item_assignee(id),
    feedback_user_id VARCHAR(80) NOT NULL,
    feedback_user_name VARCHAR(120) NOT NULL,
    progress_percent INTEGER NOT NULL DEFAULT 0,
    content TEXT NOT NULL,
    risk_note TEXT,
    attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    feedback_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS reminder_record (
    id UUID PRIMARY KEY,
    item_id UUID NOT NULL REFERENCES supervision_item(id) ON DELETE CASCADE,
    assignee_id UUID REFERENCES item_assignee(id),
    template_id UUID REFERENCES message_template(id),
    channel VARCHAR(40) NOT NULL DEFAULT 'im',
    receiver_user_id VARCHAR(80) NOT NULL,
    receiver_name VARCHAR(120) NOT NULL,
    message_title VARCHAR(300),
    message_body TEXT NOT NULL,
    send_status VARCHAR(40) NOT NULL DEFAULT 'pending',
    send_result JSONB NOT NULL DEFAULT '{}'::jsonb,
    sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS user_mapping (
    id UUID PRIMARY KEY,
    internal_user_id VARCHAR(80) NOT NULL UNIQUE,
    user_name VARCHAR(120) NOT NULL,
    department_id VARCHAR(80),
    department_name VARCHAR(120),
    external_user_ids JSONB NOT NULL DEFAULT '{}'::jsonb,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS ai_tool_call_log (
    id UUID PRIMARY KEY,
    request_id VARCHAR(80) NOT NULL,
    thread_id VARCHAR(80),
    agent_key VARCHAR(80) NOT NULL,
    tool_name VARCHAR(120) NOT NULL,
    input_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    output_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(40) NOT NULL,
    error_message TEXT,
    duration_ms INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_supervision_item_batch_id ON supervision_item(batch_id);
CREATE INDEX IF NOT EXISTS idx_supervision_import_error_batch_id ON supervision_import_error(batch_id);
CREATE INDEX IF NOT EXISTS idx_supervision_item_status ON supervision_item(status);
CREATE INDEX IF NOT EXISTS idx_supervision_item_deadline_at ON supervision_item(deadline_at);
CREATE INDEX IF NOT EXISTS idx_item_assignee_item_id ON item_assignee(item_id);
CREATE INDEX IF NOT EXISTS idx_item_assignee_user_id ON item_assignee(assignee_user_id);
CREATE INDEX IF NOT EXISTS idx_item_assignee_assigned_by_user_id ON item_assignee(assigned_by_user_id);
CREATE INDEX IF NOT EXISTS idx_progress_feedback_item_id ON progress_feedback(item_id);
CREATE INDEX IF NOT EXISTS idx_reminder_record_item_id ON reminder_record(item_id);
CREATE INDEX IF NOT EXISTS idx_ai_tool_call_log_request_id ON ai_tool_call_log(request_id);
CREATE INDEX IF NOT EXISTS idx_app_user_session_token_hash ON app_user(session_token_hash);
CREATE INDEX IF NOT EXISTS idx_app_user_department_role ON app_user(department_id, role_key);
