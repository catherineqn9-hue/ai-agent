from fastapi.testclient import TestClient

from app.agent.actions import run_action
from app.main import app


client = TestClient(app)


def test_clarify_then_resume_flow():
    first = client.post(
        "/api/v1/chat/run",
        json={"user_id": "u001", "message": "帮我跟进一下这个事情"},
    )
    assert first.status_code == 200
    first_data = first.json()
    assert first_data["need_clarification"] is True
    assert first_data["questions"]

    second = client.post(
        "/api/v1/chat/resume",
        json={
            "user_id": "u001",
            "thread_id": first_data["thread_id"],
            "message": "是关于OA权限迁移的，涉及研发部，月底前完成",
        },
    )
    assert second.status_code == 200
    second_data = second.json()
    assert second_data["need_clarification"] is False
    assert "OA" in second_data["answer"]
    assert second_data["slots"]["owner"] == "研发部"


def test_state_endpoint():
    response = client.post(
        "/api/v1/chat/run",
        json={"user_id": "u002", "message": "OA权限迁移涉及研发部，月底前完成"},
    )
    thread_id = response.json()["thread_id"]
    state = client.get(f"/api/v1/chat/state/{thread_id}")
    assert state.status_code == 200
    assert state.json()["state"]["thread_id"] == thread_id


def test_demo_page_available():
    response = client.get("/demo")
    assert response.status_code == 200
    assert "Sherry 对话智能体 MVP 演示" in response.text


def test_action_registry_extension_points():
    clarify = run_action(
        "clarify",
        {"questions": ["具体事项是什么？", "涉及谁？"]},
    )
    assert clarify.status == "waiting_user"
    assert clarify.questions

    draft = run_action(
        "oa_task_draft",
        {"slots": {"task": "OA 权限迁移", "owner": "研发部", "deadline": "月底"}},
    )
    assert draft.status == "draft_ready"
    assert draft.events[0]["type"] == "oa_task_draft_created"
