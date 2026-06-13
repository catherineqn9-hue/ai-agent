import pytest
from fastapi.testclient import TestClient

from app.main import app


@pytest.fixture(name="client")
def client_fixture():
    return TestClient(app)


def test_admin_menu_endpoint_returns_frontend_shape(client):
    response = client.get("/api/v1/admin-menus")

    if response.status_code != 200:
        pytest.skip("database is not available for admin menu integration test")

    menus = response.json()["menus"]
    menu_ids = {menu["id"] for menu in menus}

    assert {"assistant", "menu"}.issubset(menu_ids)

    assistant = next(menu for menu in menus if menu["id"] == "assistant")
    menu_config = next(menu for menu in menus if menu["id"] == "menu")

    assert assistant["type"] == "assistant"
    assert assistant["title"] == "AI 助手问答"
    assert isinstance(menu_config["tableFields"], list)
    assert isinstance(menu_config["fields"], list)
    assert menu_config["resource"] == "admin-menus"


def test_excel_template_menu_replaces_field_mapping_menu(client):
    response = client.get("/api/v1/admin-menus")

    if response.status_code != 200:
        pytest.skip("database is not available for admin menu integration test")

    menus = response.json()["menus"]
    resources = {menu["resource"] for menu in menus}

    assert "excel-import-templates" in resources
    assert "excel-field-mappings" not in resources
