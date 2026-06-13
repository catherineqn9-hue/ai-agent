from app.agent.framework import get_agent_runtime


def test_framework_config_endpoint_lists_agents():
    from app.main import app
    from fastapi.testclient import TestClient

    client = TestClient(app)
    response = client.get("/api/v1/framework/config")

    assert response.status_code == 200
    data = response.json()
    assert data["runtime"]["default_provider"] == "langchain"
    assert "task_parser" in data["agents"]
    assert "workflow_orchestrator" in data["registered_agents"]


def test_framework_dispatch_endpoint_runs_agent_hook():
    from app.main import app
    from fastapi.testclient import TestClient

    client = TestClient(app)
    response = client.post(
        "/api/v1/framework/dispatch/task_parser",
        json={"task_text": "parse this supervision task"},
    )

    assert response.status_code == 200
    data = response.json()
    assert data["agent"] == "task_parser"
    assert data["status"] == "ready"
    assert data["output"]["supervision_events"]["task_text"] == "parse this supervision task"
    assert data["events"][0]["type"] == "agent_hook_ready"


def test_agent_runtime_enabled_agents_are_registered():
    runtime = get_agent_runtime()

    enabled = runtime.enabled_agents()

    assert set(enabled) == {
        "task_parser",
        "owner_matcher",
        "progress_monitor",
        "reminder_generator",
        "workflow_orchestrator",
    }
    assert set(enabled).issubset(set(runtime.registry.names()))
