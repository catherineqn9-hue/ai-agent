-- Keep this migration idempotent because the Python MVP may have already created tables.
CREATE TABLE IF NOT EXISTS supervision_item (
    id UUID PRIMARY KEY,
    batch_id UUID,
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

CREATE TABLE IF NOT EXISTS progress_feedback (
    id UUID PRIMARY KEY,
    item_id UUID NOT NULL REFERENCES supervision_item(id) ON DELETE CASCADE,
    assignee_id UUID,
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

ALTER TABLE app_user ADD COLUMN IF NOT EXISTS department_id VARCHAR(80) NOT NULL DEFAULT 'operations';
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS department_name VARCHAR(120) NOT NULL DEFAULT '运营部';
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS role_key VARCHAR(60) NOT NULL DEFAULT 'member';
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS role_name VARCHAR(120) NOT NULL DEFAULT '成员';

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

ALTER TABLE item_assignee ADD COLUMN IF NOT EXISTS assigned_by_user_id VARCHAR(80) NOT NULL DEFAULT 'system';
ALTER TABLE item_assignee ADD COLUMN IF NOT EXISTS assigned_by_name VARCHAR(120) NOT NULL DEFAULT '系统';
ALTER TABLE item_assignee ADD COLUMN IF NOT EXISTS assignment_note TEXT;
ALTER TABLE item_assignee ADD COLUMN IF NOT EXISTS rejection_reason TEXT;
ALTER TABLE item_assignee ADD COLUMN IF NOT EXISTS assigned_at TIMESTAMPTZ NOT NULL DEFAULT now();

CREATE TABLE IF NOT EXISTS supervision_import_error (
    id UUID PRIMARY KEY,
    batch_id UUID NOT NULL,
    row_no INTEGER,
    item_no VARCHAR(80),
    title VARCHAR(300),
    error_message TEXT NOT NULL,
    raw_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_supervision_import_error_batch_id ON supervision_import_error(batch_id);
CREATE INDEX IF NOT EXISTS idx_app_user_session_token_hash ON app_user(session_token_hash);
CREATE INDEX IF NOT EXISTS idx_app_user_department_role ON app_user(department_id, role_key);
CREATE INDEX IF NOT EXISTS idx_item_assignee_item_id ON item_assignee(item_id);
CREATE INDEX IF NOT EXISTS idx_item_assignee_user_id ON item_assignee(assignee_user_id);
CREATE INDEX IF NOT EXISTS idx_item_assignee_assigned_by_user_id ON item_assignee(assigned_by_user_id);
