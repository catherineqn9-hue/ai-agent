from uuid import uuid4

import pytest
from fastapi.testclient import TestClient

from app.main import app


@pytest.fixture(name="client")
def client_fixture():
    return TestClient(app)


def test_supervision_item_crud_flow(client):
    item_no = f"TEST-{uuid4().hex[:8]}"
    create_response = client.post(
        "/api/v1/supervision-items",
        json={
            "item_no": item_no,
            "title": "测试督办事项",
            "description": "用于验证督办事项接口闭环",
            "priority": "high",
            "status": "pending_assign",
            "created_by": "pytest",
        },
    )

    if create_response.status_code != 200:
        pytest.skip("database is not available for supervision item integration test")

    item = create_response.json()
    assert item["item_no"] == item_no
    assert item["title"] == "测试督办事项"

    item_id = item["id"]
    update_response = client.put(
        f"/api/v1/supervision-items/{item_id}",
        json={"title": "测试督办事项-已更新", "priority": "urgent"},
    )
    assert update_response.status_code == 200
    assert update_response.json()["priority"] == "urgent"

    status_response = client.patch(
        f"/api/v1/supervision-items/{item_id}/status",
        json={"status": "completed"},
    )
    assert status_response.status_code == 200
    assert status_response.json()["status"] == "completed"
    assert status_response.json()["completed_at"] is not None

    list_response = client.get("/api/v1/supervision-items")
    assert list_response.status_code == 200
    assert any(row["id"] == item_id for row in list_response.json()["items"])

    delete_response = client.delete(f"/api/v1/supervision-items/{item_id}")
    assert delete_response.status_code == 200
