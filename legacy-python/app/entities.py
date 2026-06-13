from datetime import datetime
from enum import StrEnum
from typing import Any
from uuid import UUID

from pydantic import BaseModel, Field


class ItemStatus(StrEnum):
    PENDING_ASSIGN = "pending_assign"
    PENDING_CONFIRM = "pending_confirm"
    IN_PROGRESS = "in_progress"
    NEAR_DUE = "near_due"
    REMINDED = "reminded"
    COMPLETED = "completed"
    OVERDUE = "overdue"
    CANCELLED = "cancelled"


class ImportStatus(StrEnum):
    PENDING = "pending"
    PARSING = "parsing"
    COMPLETED = "completed"
    FAILED = "failed"


class SendStatus(StrEnum):
    PENDING = "pending"
    SENT = "sent"
    FAILED = "failed"


class BaseEntity(BaseModel):
    id: UUID
    created_at: datetime | None = None
    updated_at: datetime | None = None


class AgentConfigEntity(BaseEntity):
    agent_key: str
    agent_name: str
    agent_type: str
    provider: str = "langgraph"
    model_name: str | None = None
    system_prompt: str | None = None
    tool_permissions: list[str] = Field(default_factory=list)
    parameters: dict[str, Any] = Field(default_factory=dict)
    enabled: bool = True


class IntegrationConfigEntity(BaseEntity):
    integration_key: str
    integration_name: str
    integration_type: str
    base_url: str | None = None
    auth_type: str = "none"
    auth_config: dict[str, Any] = Field(default_factory=dict)
    timeout_seconds: int = 10
    enabled: bool = False


class MessageTemplateEntity(BaseEntity):
    template_key: str
    template_name: str
    scene: str
    channel: str = "im"
    title_template: str | None = None
    body_template: str
    variables: list[str] = Field(default_factory=list)
    enabled: bool = True


class ExcelFieldMappingEntity(BaseEntity):
    mapping_key: str
    mapping_name: str
    source_column: str
    target_field: str
    required: bool = False
    transform_rule: dict[str, Any] = Field(default_factory=dict)
    enabled: bool = True


class ExcelImportTemplateEntity(BaseEntity):
    template_code: str
    template_name: str
    description: str | None = None
    handler_code: str
    source_columns: list[str] = Field(default_factory=list)
    entity_fields: list[str] = Field(default_factory=list)
    mapping_config: dict[str, Any] = Field(default_factory=dict)
    enabled: bool = True


class ReminderRuleEntity(BaseEntity):
    rule_key: str
    rule_name: str
    trigger_type: str
    days_before_deadline: int = 1
    repeat_interval_hours: int = 24
    max_send_count: int = 3
    skip_holidays: bool = True
    enabled: bool = True


class StatusDictEntity(BaseEntity):
    status_key: str
    status_name: str
    status_group: str = "supervision_item"
    sort_order: int = 0
    is_terminal: bool = False
    enabled: bool = True


class AdminMenuConfigEntity(BaseEntity):
    menu_id: str
    title: str
    icon: str
    menu_type: str
    resource: str | None = None
    hint: str | None = None
    group_name: str | None = None
    sort_order: int = 0
    table_fields: list[str] = Field(default_factory=list)
    form_fields: list[dict[str, Any]] = Field(default_factory=list)
    enabled: bool = True


class SupervisionBatchEntity(BaseEntity):
    batch_no: str
    batch_name: str
    source_type: str = "excel"
    source_file_id: str | None = None
    import_status: ImportStatus = ImportStatus.PENDING
    total_count: int = 0
    success_count: int = 0
    failed_count: int = 0
    created_by: str


class SupervisionItemEntity(BaseEntity):
    batch_id: UUID | None = None
    item_no: str
    title: str
    description: str | None = None
    source_row_no: int | None = None
    priority: str = "normal"
    status: ItemStatus = ItemStatus.PENDING_ASSIGN
    deadline_at: datetime | None = None
    completed_at: datetime | None = None
    created_by: str


class ItemAssigneeEntity(BaseEntity):
    item_id: UUID
    assignee_user_id: str
    assignee_name: str
    department_id: str | None = None
    department_name: str | None = None
    role_type: str = "owner"
    confirm_status: str = "pending"
    confirmed_at: datetime | None = None


class ProgressFeedbackEntity(BaseModel):
    id: UUID
    item_id: UUID
    assignee_id: UUID | None = None
    feedback_user_id: str
    feedback_user_name: str
    progress_percent: int = Field(default=0, ge=0, le=100)
    content: str
    risk_note: str | None = None
    attachment_ids: list[str] = Field(default_factory=list)
    feedback_at: datetime | None = None
    created_at: datetime | None = None


class ReminderRecordEntity(BaseModel):
    id: UUID
    item_id: UUID
    assignee_id: UUID | None = None
    template_id: UUID | None = None
    channel: str = "im"
    receiver_user_id: str
    receiver_name: str
    message_title: str | None = None
    message_body: str
    send_status: SendStatus = SendStatus.PENDING
    send_result: dict[str, Any] = Field(default_factory=dict)
    sent_at: datetime | None = None
    created_at: datetime | None = None


class UserMappingEntity(BaseEntity):
    internal_user_id: str
    user_name: str
    department_id: str | None = None
    department_name: str | None = None
    external_user_ids: dict[str, str] = Field(default_factory=dict)
    enabled: bool = True


class AiToolCallLogEntity(BaseModel):
    id: UUID
    request_id: str
    thread_id: str | None = None
    agent_key: str
    tool_name: str
    input_payload: dict[str, Any] = Field(default_factory=dict)
    output_payload: dict[str, Any] = Field(default_factory=dict)
    status: str
    error_message: str | None = None
    duration_ms: int | None = None
    created_at: datetime | None = None
