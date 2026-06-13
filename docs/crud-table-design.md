# CRUD Table Design

The AI agent should call backend APIs instead of changing business data directly. These tables are the initial API resource boundaries.

## Configuration

| Table | Purpose |
|---|---|
| `agent_config` | Agent switch, provider, prompt, and tool permission configuration |
| `integration_config` | User service, IM service, OA service, and file service endpoint configuration |
| `message_template` | Confirmation, due reminder, overdue reminder, and feedback message templates |
| `excel_field_mapping` | Excel column to business field mapping |
| `reminder_rule` | Reminder trigger timing, frequency, and max send count |

## Supervision Business

| Table | Purpose |
|---|---|
| `supervision_batch` | One Excel import or one generated supervision batch |
| `supervision_item` | One trackable supervision item parsed from the batch |
| `item_assignee` | Owner, collaborator, and department assignment for an item |
| `progress_feedback` | Progress feedback submitted by assignees |
| `reminder_record` | Reminder send records and result payloads |

## Integration And Audit

| Table | Purpose |
|---|---|
| `user_mapping` | Internal user and external user ID mapping |
| `ai_tool_call_log` | AI tool/API call audit log |

## Next API Resources

- `/api/v1/agent-configs`
- `/api/v1/integration-configs`
- `/api/v1/message-templates`
- `/api/v1/supervision-batches`
- `/api/v1/supervision-items`
- `/api/v1/supervision-items/{item_id}/assignees`
- `/api/v1/supervision-items/{item_id}/feedback`
- `/api/v1/supervision-items/{item_id}/reminders`
