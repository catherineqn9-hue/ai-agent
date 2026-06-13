from uuid import uuid4

from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)


def test_admin_page_available():
    response = client.get("/admin")

    assert response.status_code == 200
    assert "Sherry 智能督办后台" in response.text
    assert "AI 助手问答" in response.text


def test_business_config_crud_flow():
    key = f"pytest_{uuid4().hex}"

    created = client.post(
        "/api/v1/configs",
        json={
            "config_type": "agent",
            "key": key,
            "name": "Pytest Agent",
            "description": "created by test",
            "enabled": True,
            "data": {"provider": "langchain"},
        },
    )
    assert created.status_code == 200
    item = created.json()

    listed = client.get("/api/v1/configs?config_type=agent")
    assert listed.status_code == 200
    assert any(config["id"] == item["id"] for config in listed.json()["items"])

    updated = client.put(
        f"/api/v1/configs/{item['id']}",
        json={"name": "Updated Pytest Agent", "enabled": False},
    )
    assert updated.status_code == 200
    assert updated.json()["name"] == "Updated Pytest Agent"
    assert updated.json()["enabled"] is False

    deleted = client.delete(f"/api/v1/configs/{item['id']}")
    assert deleted.status_code == 200
    assert deleted.json()["deleted"] == item["id"]

    missing = client.get(f"/api/v1/configs/{item['id']}")
    assert missing.status_code == 404
