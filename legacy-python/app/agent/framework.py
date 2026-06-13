from dataclasses import dataclass, field
from typing import Any, Protocol

from app.config import AgentConfig, FrameworkConfig, get_framework_config


@dataclass(frozen=True)
class AgentResult:
    agent: str
    status: str
    output: dict[str, Any] = field(default_factory=dict)
    events: list[dict[str, Any]] = field(default_factory=list)
    trace: list[dict[str, str]] = field(default_factory=list)


class SupervisionAgent(Protocol):
    name: str

    def run(self, payload: dict[str, Any]) -> AgentResult:
        ...


class AgentRegistry:
    def __init__(self) -> None:
        self._agents: dict[str, SupervisionAgent] = {}

    def register(self, agent: SupervisionAgent) -> None:
        self._agents[agent.name] = agent

    def get(self, name: str) -> SupervisionAgent:
        try:
            return self._agents[name]
        except KeyError as exc:
            raise KeyError(f"Agent is not registered: {name}") from exc

    def names(self) -> list[str]:
        return sorted(self._agents)


class AgentRuntime:
    def __init__(self, config: FrameworkConfig, registry: AgentRegistry) -> None:
        self.config = config
        self.registry = registry

    def enabled_agents(self) -> dict[str, AgentConfig]:
        return {
            name: agent_config
            for name, agent_config in self.config.agents.items()
            if agent_config.enabled
        }

    def describe(self) -> dict[str, Any]:
        return {
            "runtime": self.config.runtime.model_dump(),
            "plugins": [plugin.model_dump() for plugin in self.config.plugins if plugin.enabled],
            "agents": {
                name: agent_config.model_dump()
                for name, agent_config in self.enabled_agents().items()
            },
            "registered_agents": self.registry.names(),
            "integrations": {
                name: integration.model_dump()
                for name, integration in self.config.integrations.items()
            },
        }

    def dispatch(self, agent_name: str, payload: dict[str, Any]) -> AgentResult:
        agent_config = self.config.agents.get(agent_name)
        if agent_config is None:
            raise KeyError(f"Agent is not configured: {agent_name}")
        if not agent_config.enabled:
            raise ValueError(f"Agent is disabled: {agent_name}")

        agent = self.registry.get(agent_name)
        result = agent.run(payload)
        if self.config.runtime.trace_enabled:
            trace = [
                *result.trace,
                {
                    "node": "agent_runtime",
                    "detail": f"dispatched {agent_name} via {agent_config.provider}",
                },
            ]
            return AgentResult(
                agent=result.agent,
                status=result.status,
                output=result.output,
                events=result.events,
                trace=trace,
            )
        return result


agent_registry = AgentRegistry()


def get_agent_runtime() -> AgentRuntime:
    from app.agent import service_agents  # noqa: F401 - ensures built-in agents are registered

    return AgentRuntime(config=get_framework_config(), registry=agent_registry)
