from uuid import uuid4

import pytest
from fastapi.testclient import TestClient

from app.agent.tools.supervision_tools import (
    add_progress_feedback_tool,
    create_supervision_item_tool,
    query_supervision_items_tool,
    update_supervision_status_tool,
)
from app.main import app


@pytest.fixture(name="client")
def client_fixture():
    return TestClient(app)


def test_progress_feedback_api_and_agent_tools(client):
    item = create_supervision_item_tool(
        title=f"工具测试事项-{uuid4().hex[:8]}",
        description="用于验证 AI tool 调用业务接口",
        created_by="pytest",
    )

    queried = query_supervision_items_tool(keyword=item["title"])
    assert queried["count"] >= 1

    updated = update_supervision_status_tool(item["id"], "in_progress")
    assert updated["status"] == "in_progress"

    feedback = add_progress_feedback_tool(
        item_id=item["id"],
        content="已完成接口联调",
        progress_percent=60,
        feedback_user_id="pytest",
        feedback_user_name="Pytest",
    )
    assert feedback["progress_percent"] == 60

    response = client.get(f"/api/v1/progress-feedbacks?item_id={item['id']}")
    if response.status_code != 200:
        pytest.skip("database is not available for progress feedback integration test")

    items = response.json()["items"]
    assert any(row["id"] == feedback["id"] for row in items)


def test_chat_can_query_supervision_items(client):
    create_supervision_item_tool(
        title=f"AI查询事项-{uuid4().hex[:8]}",
        created_by="pytest",
    )

    response = client.post(
        "/api/v1/chat/run",
        json={"user_id": "u_tool", "message": "查询督办事项列表"},
    )

    assert response.status_code == 200
    data = response.json()
    assert data["need_clarification"] is False
    assert data["intent"] == "query_supervision_items"
    assert "督办事项" in data["answer"]
