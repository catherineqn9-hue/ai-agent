from typing import Any

from app.agent.framework import AgentResult, agent_registry


class BaseConfiguredAgent:
    name = ""
    output_key = "result"

    def run(self, payload: dict[str, Any]) -> AgentResult:
        return AgentResult(
            agent=self.name,
            status="ready",
            output={
                self.output_key: payload,
                "message": f"{self.name} framework hook is ready.",
            },
            events=[{"type": "agent_hook_ready", "agent": self.name}],
            trace=[{"node": self.name, "detail": "executed configured agent hook"}],
        )


class TaskParserAgent(BaseConfiguredAgent):
    name = "task_parser"
    output_key = "supervision_events"


class OwnerMatcherAgent(BaseConfiguredAgent):
    name = "owner_matcher"
    output_key = "assigned_events"


class ProgressMonitorAgent(BaseConfiguredAgent):
    name = "progress_monitor"
    output_key = "event_status_updates"


class ReminderGeneratorAgent(BaseConfiguredAgent):
    name = "reminder_generator"
    output_key = "reminder_messages"


class WorkflowOrchestratorAgent(BaseConfiguredAgent):
    name = "workflow_orchestrator"
    output_key = "workflow_result"


def register_service_agents() -> None:
    for agent in [
        TaskParserAgent(),
        OwnerMatcherAgent(),
        ProgressMonitorAgent(),
        ReminderGeneratorAgent(),
        WorkflowOrchestratorAgent(),
    ]:
        agent_registry.register(agent)


register_service_agents()
