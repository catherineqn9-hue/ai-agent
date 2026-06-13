from functools import lru_cache
from pathlib import Path
from typing import Any

import yaml
from pydantic import BaseModel, Field


PROJECT_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_AGENT_CONFIG = PROJECT_ROOT / "config" / "agents.yaml"


class RuntimeConfig(BaseModel):
    default_provider: str = "langchain"
    fallback_provider: str | None = "agentscope"
    trace_enabled: bool = True


class PluginConfig(BaseModel):
    name: str
    enabled: bool = True
    description: str = ""
    agents: list[str] = Field(default_factory=list)


class AgentConfig(BaseModel):
    enabled: bool = True
    provider: str = "langchain"
    role: str
    description: str = ""
    inputs: list[str] = Field(default_factory=list)
    outputs: list[str] = Field(default_factory=list)


class IntegrationConfig(BaseModel):
    enabled: bool = False
    base_url: str = ""


class FrameworkConfig(BaseModel):
    runtime: RuntimeConfig = Field(default_factory=RuntimeConfig)
    plugins: list[PluginConfig] = Field(default_factory=list)
    agents: dict[str, AgentConfig] = Field(default_factory=dict)
    integrations: dict[str, IntegrationConfig] = Field(default_factory=dict)


def load_framework_config(path: Path = DEFAULT_AGENT_CONFIG) -> FrameworkConfig:
    if not path.exists():
        raise FileNotFoundError(f"Agent config not found: {path}")

    raw: dict[str, Any] = yaml.safe_load(path.read_text(encoding="utf-8")) or {}
    return FrameworkConfig.model_validate(raw)


@lru_cache(maxsize=1)
def get_framework_config() -> FrameworkConfig:
    return load_framework_config()
