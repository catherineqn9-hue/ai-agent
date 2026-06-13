from uuid import uuid4

import pytest
from fastapi.testclient import TestClient

from app.db import get_connection
from app.main import app


client = TestClient(app)


def db_available() -> bool:
    try:
        with get_connection() as conn:
            with conn.cursor() as cur:
                cur.execute("SELECT 1")
                return cur.fetchone() is not None
    except Exception:
        return False


pytestmark = pytest.mark.skipif(not db_available(), reason="PostgreSQL is not available")


def test_basic_config_agent_crud_flow():
    key = f"agent_{uuid4().hex}"

    created = client.post(
        "/api/v1/basic-configs/agent-configs",
        json={
            "agent_key": key,
            "agent_name": "Pytest Agent",
            "agent_type": "task_parser",
            "provider": "langgraph",
            "tool_permissions": ["parse_excel"],
            "parameters": {"temperature": 0},
            "enabled": True,
        },
    )
    assert created.status_code == 200
    item = created.json()
    assert item["agent_key"] == key

    listed = client.get("/api/v1/basic-configs/agent-configs")
    assert listed.status_code == 200
    assert any(config["id"] == item["id"] for config in listed.json()["items"])

    updated = client.put(
        f"/api/v1/basic-configs/agent-configs/{item['id']}",
        json={"agent_name": "Updated Agent", "enabled": False},
    )
    assert updated.status_code == 200
    assert updated.json()["agent_name"] == "Updated Agent"
    assert updated.json()["enabled"] is False

    deleted = client.delete(f"/api/v1/basic-configs/agent-configs/{item['id']}")
    assert deleted.status_code == 200
    assert deleted.json()["deleted"] == item["id"]


def test_basic_config_status_dict_crud_flow():
    key = f"status_{uuid4().hex}"

    created = client.post(
        "/api/v1/basic-configs/status-dicts",
        json={
            "status_key": key,
            "status_name": "Pytest Status",
            "status_group": "supervision_item",
            "sort_order": 99,
            "is_terminal": False,
            "enabled": True,
        },
    )
    assert created.status_code == 200
    item = created.json()
    assert item["status_key"] == key

    deleted = client.delete(f"/api/v1/basic-configs/status-dicts/{item['id']}")
    assert deleted.status_code == 200
