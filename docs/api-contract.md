# API Contract

All JSON APIs return a standard response envelope.

```json
{
  "success": true,
  "code": 0,
  "message": "ok",
  "data": {},
  "trace_id": "request-trace-id"
}
```

## Business Codes

| Code | HTTP | Meaning |
|---:|---:|---|
| `0` | `200` | Success |
| `40000` | `400` | Invalid parameter or malformed file |
| `40100` | `401` | Unauthenticated, reserved |
| `40300` | `403` | Forbidden, reserved |
| `40400` | `404` | Resource not found |
| `40900` | `409` | Business conflict, duplicate key, invalid state transition |
| `50000` | `500` | System error |

## Supervision Item Status

Valid statuses:

| Status | Name | Terminal |
|---|---|---|
| `pending_assign` | 待分派 | No |
| `in_progress` | 进行中 | No |
| `blocked` | 受阻 | No |
| `completed` | 已完成 | Yes |
| `cancelled` | 已取消 | Yes |

The backend stores the stable `status` code for workflow transitions and returns `status_name` for Chinese display. Create/update/status APIs accept either the status code or the Chinese name.

Allowed transitions:

| From | To |
|---|---|
| `pending_assign` | `pending_assign`, `in_progress`, `cancelled` |
| `in_progress` | `in_progress`, `blocked`, `completed`, `cancelled` |
| `blocked` | `blocked`, `in_progress`, `cancelled` |
| `completed` | `completed` |
| `cancelled` | `cancelled` |

Invalid transitions return `HTTP 409` with `code=40900`.

## Endpoints

### Health

`GET /health`

Response `data`:

```json
{
  "status": "ok"
}
```

### Auth

`POST /api/v1/auth/register`

Request:

```json
{
  "username": "admin",
  "display_name": "管理员",
  "password": "password123"
}
```

`POST /api/v1/auth/login`

Request:

```json
{
  "username": "admin",
  "password": "password123"
}
```

Register and login response `data`:

```json
{
  "access_token": "opaque-token",
  "token_type": "Bearer",
  "user": {
    "id": "uuid",
    "username": "admin",
    "display_name": "管理员",
    "department_id": "operations",
    "department_name": "运营部",
    "role_key": "owner",
    "role_name": "主责人"
  }
}
```

`GET /api/v1/auth/me`

Header:

```text
Authorization: Bearer opaque-token
```

`POST /api/v1/auth/logout`

Header:

```text
Authorization: Bearer opaque-token
```

Passwords are stored as salted PBKDF2 hashes. Session tokens are only stored as SHA-256 hashes.

### Users

`GET /api/v1/users`

Returns local users that can participate in supervision assignment.

Response `data`:

```json
{
  "users": [
    {
      "id": "uuid",
      "username": "zhangsan",
      "display_name": "张三",
      "department_id": "operations",
      "department_name": "运营部",
      "role_key": "owner",
      "role_name": "主责人",
      "enabled": true
    }
  ]
}
```

`PUT /api/v1/users/{id}`

Updates the display name, department, role, and enabled status used by assignment routing.

Request:

```json
{
  "display_name": "张三",
  "department_id": "operations",
  "department_name": "运营部",
  "role_key": "owner",
  "role_name": "主责人",
  "enabled": true
}
```

### Admin Menus

`GET /api/v1/admin-menus`

Response `data`:

```json
{
  "menus": [
    {
      "id": "assistant",
      "title": "AI 助手问答",
      "icon": "AI",
      "type": "assistant",
      "resource": null,
      "hint": "通过对话调用后端接口能力",
      "groupName": "智能助手",
      "tableFields": [],
      "fields": []
    }
  ]
}
```

### Supervision Items

`GET /api/v1/supervision-items?status=&keyword=`

Response `data`:

```json
{
  "items": []
}
```

`POST /api/v1/supervision-items`

Request:

```json
{
  "item_no": "ITEM-001",
  "title": "整理经营周报",
  "description": "本周经营数据汇总",
  "priority": "normal",
  "status": "pending_assign",
  "deadline_at": "2026-06-30T18:00:00+08:00",
  "created_by": "admin"
}
```

Notes:

- `item_no` can be omitted; backend generates it.
- `status` must be one of the valid supervision item statuses. Chinese labels such as `进行中` are accepted and normalized before storage.
- Duplicate `item_no` returns `HTTP 409`, `code=40900`.

`PUT /api/v1/supervision-items/{id}`

Uses the same body as create. If `status` changes, backend validates the state transition.

`PATCH /api/v1/supervision-items/{id}/status`

Request:

```json
{
  "status": "completed"
}
```

`status` can also be the Chinese label, for example:

```json
{
  "status": "已完成"
}
```

`DELETE /api/v1/supervision-items/{id}`

Response `data`:

```json
{
  "deleted": "uuid"
}
```

`GET /api/v1/supervision-items/{id}/assignees`

Response `data`:

```json
{
  "assignees": [
    {
      "id": "uuid",
      "item_id": "uuid",
      "assigned_by_user_id": "manager",
      "assigned_by_name": "分配人",
      "assignee_user_id": "zhangsan",
      "assignee_name": "张三",
      "role_type": "owner",
      "role_type_name": "主责人",
      "confirm_status": "pending",
      "confirm_status_name": "待确认",
      "assignment_note": "请负责本周经营数据汇总",
      "assigned_at": "2026-06-13T16:00:00+08:00"
    }
  ]
}
```

`GET /api/v1/supervision-items/{id}/assignment-recommendations?role_type=owner&department_id=operations`

Returns AI-assist assignment candidates based on the current user role and department metadata. This endpoint only recommends candidates. It does not write assignment records. Human confirmation is still required through `POST /api/v1/supervision-items/{id}/assignees`.

Response `data`:

```json
{
  "candidates": [
    {
      "assignee_user_id": "zhangsan",
      "assignee_name": "张三",
      "department_id": "operations",
      "department_name": "运营部",
      "role_type": "owner",
      "role_name": "主责人",
      "confidence": 0.82,
      "reason": "用户角色匹配主责人，且属于当前默认部门，可作为督办分派候选。",
      "requires_human_review": true
    }
  ]
}
```

`POST /api/v1/supervision-items/{id}/assignees`

Request:

```json
{
  "assignee_user_id": "zhangsan",
  "assignee_name": "张三",
  "role_type": "owner",
  "department_id": null,
  "department_name": null,
  "assignment_note": "请负责本周经营数据汇总"
}
```

`POST /api/v1/supervision-items/{id}/confirm-receive`

Confirms the current logged-in user's assignment. If the item is still `pending_assign`, it moves to `in_progress`.

`POST /api/v1/supervision-items/{id}/reject-assignment`

Request:

```json
{
  "rejection_reason": "当前不属于我的职责范围"
}
```

### My Supervision Console

`GET /api/v1/my/supervision-items`

Returns supervision items assigned to the current logged-in user.

Response `data`:

```json
{
  "items": [
    {
      "item": {
        "id": "uuid",
        "item_no": "ITEM-001",
        "title": "整理经营周报",
        "priority": "normal",
        "priority_name": "普通",
        "status": "in_progress",
        "status_name": "进行中"
      },
      "assignment": {
        "assigned_by_user_id": "manager",
        "assigned_by_name": "分配人",
        "assignee_user_id": "zhangsan",
        "assignee_name": "张三",
        "confirm_status": "pending",
        "confirm_status_name": "待确认"
      },
      "risk_level": "medium",
      "requires_human_review": false
    }
  ]
}
```

`GET /api/v1/my/supervision-items/{id}`

Returns the current user's assignment, all assignees, and progress feedbacks for the item.

### Progress Feedbacks

`GET /api/v1/progress-feedbacks?item_id=`

Response `data`:

```json
{
  "items": []
}
```

`POST /api/v1/progress-feedbacks`

Request:

```json
{
  "item_id": "uuid",
  "feedback_user_id": "admin_user",
  "feedback_user_name": "AI 助手",
  "progress_percent": 80,
  "content": "负责人已接收",
  "risk_note": "等待回执"
}
```

### Excel Imports

`GET /api/v1/supervision-imports/templates`

Response `data`:

```json
{
  "templates": [
    {
      "template_code": "standard_supervision",
      "template_name": "标准督办事项模板",
      "handler_code": "standard_supervision",
      "source_columns": ["事项编号", "事项标题"],
      "entity_fields": ["item_no", "title"]
    }
  ]
}
```

`GET /api/v1/supervision-imports/batches`

Response `data`:

```json
{
  "batches": [
    {
      "id": "uuid",
      "batch_no": "BATCH-20260612120000000",
      "batch_name": "标准督办事项模板 - demo.xlsx",
      "source_type": "excel",
      "source_file_id": "demo.xlsx",
      "import_status": "failed",
      "total_count": 10,
      "success_count": 8,
      "failed_count": 2,
      "created_by": "admin"
    }
  ]
}
```

`GET /api/v1/supervision-imports/batches/{batch_id}`

Response `data`:

```json
{
  "batch": {},
  "items": [],
  "errors": [
    {
      "id": "uuid",
      "batch_id": "uuid",
      "row_no": 3,
      "item_no": "ITEM-001",
      "title": "重复事项",
      "error_message": "第 3 行导入失败：事项编号重复：ITEM-001",
      "raw_data": {}
    }
  ]
}
```

`GET /api/v1/supervision-imports/batches/{batch_id}/errors`

Response `data`:

```json
{
  "errors": []
}
```

`POST /api/v1/supervision-imports/excel`

Form data:

| Field | Required | Description |
|---|---|---|
| `file` | Yes | `.xlsx` or `.xls` file |
| `template_code` | Yes | Template code |
| `created_by` | No | Import operator, default `admin` |

Response `data`:

```json
{
  "batch_id": "uuid",
  "batch_no": "BATCH-20260612120000000",
  "batch_name": "标准督办事项模板 - demo.xlsx",
  "import_status": "completed",
  "total_count": 10,
  "success_count": 10,
  "failed_count": 0,
  "errors": []
}
```

### Chat

`POST /api/v1/chat/run`

Request:

```json
{
  "user_id": "admin_user",
  "message": "查询督办事项列表",
  "thread_id": null,
  "context": {
    "status": "pending_assign"
  }
}
```

Response `data`:

```json
{
  "request_id": "uuid",
  "thread_id": "uuid",
  "answer": "查到 3 条督办事项...",
  "need_clarification": false,
  "questions": [],
  "intent": "query_supervision_items",
  "slots": {},
  "status": "completed",
  "trace_id": "uuid",
  "trace": []
}
```

Supported intents:

- `query_supervision_items`: 查询督办事项，支持 `context.status`、`context.keyword`
- `create_supervision_item`: 创建督办事项，支持 `context.title`、`context.description`、`context.priority`
- `update_supervision_status`: 更新督办事项状态，必传 `context.item_id`，可传 `context.status`
- `progress_feedback`: 添加进度反馈，必传 `context.item_id`，可传 `context.content`、`context.progress_percent`、`context.risk_note`
- `query_progress_feedbacks`: 查询事项进度反馈，必传 `context.item_id`
- `query_import_batches`: 查询最近 Excel 导入批次
- `query_import_batch_detail`: 查询导入批次事项和失败明细，必传 `context.batch_id`

Import batch detail request example:

```json
{
  "user_id": "admin_user",
  "message": "查询导入批次失败明细",
  "thread_id": null,
  "context": {
    "batch_id": "uuid"
  }
}
```

Kimi model configuration:

- `KIMI_API_KEY`: Moonshot/Kimi API key. Keep it in environment variables, not source code.
- `KIMI_BASE_URL`: defaults to `https://api.moonshot.ai/v1`.
- `KIMI_MODEL`: defaults to `moonshot-v1-8k`.

AI tool call audit:

- Business actions executed by the assistant are recorded in `ai_tool_call_log`.
- `agent_key` uses `supervision_assistant`.
- `tool_name` stores the business capability, such as `query_import_batches` or `create_supervision_item`.
- `input_payload` and `output_payload` are stored as JSONB for later review pages and troubleshooting.
