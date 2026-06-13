# Sherry Agent Framework

This skeleton provides the configurable foundation for the supervision-service agents.

## What is included

- `config/agents.yaml`: plugin, agent, runtime, and integration switches.
- `app/config.py`: typed config loader with Pydantic validation.
- `app/agent/framework.py`: registry, runtime, dispatch result, and trace handling.
- `app/agent/service_agents.py`: first supervision-agent hooks:
  - `task_parser`
  - `owner_matcher`
  - `progress_monitor`
  - `reminder_generator`
  - `workflow_orchestrator`

## API

- `GET /api/v1/framework/config`: inspect enabled plugins, configured agents, registered agents, and integrations.
- `POST /api/v1/framework/dispatch/{agent_name}`: run one configured agent hook with a JSON payload.

The hooks return `ready` today. The next step is to replace each hook body with the real Excel parser, user-service matcher, status tracker, IM reminder adapter, and full workflow orchestration.
