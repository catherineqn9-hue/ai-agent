from io import BytesIO
from uuid import uuid4

import pytest
from fastapi.testclient import TestClient
from openpyxl import Workbook

from app.main import app


@pytest.fixture(name="client")
def client_fixture():
    return TestClient(app)


def test_excel_import_creates_batch_and_items(client):
    templates_response = client.get("/api/v1/supervision-imports/templates")
    if templates_response.status_code != 200:
        pytest.skip("database is not available for supervision import integration test")

    templates = templates_response.json()["templates"]
    assert any(template["template_code"] == "standard_supervision" for template in templates)
    standard_template = next(
        template for template in templates if template["template_code"] == "standard_supervision"
    )
    source_columns = standard_template["source_columns"]
    entity_fields = standard_template["entity_fields"]
    title_index = entity_fields.index("title")

    assert source_columns[title_index] == "事项标题"
    assert len(source_columns) == len(entity_fields)

    title = f"Excel导入事项-{uuid4().hex[:8]}"
    workbook = Workbook()
    sheet = workbook.active
    sheet.append(["事项编号", "事项标题", "描述", "优先级", "状态", "截止时间", "创建人"])
    sheet.append(
        [
            f"IMP-{uuid4().hex[:8]}",
            title,
            "导入描述",
            "high",
            "pending_assign",
            "2026-06-30",
            "pytest",
        ]
    )

    buffer = BytesIO()
    workbook.save(buffer)
    buffer.seek(0)

    response = client.post(
        "/api/v1/supervision-imports/excel",
        files={
            "file": (
                "items.xlsx",
                buffer.getvalue(),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            )
        },
        data={"created_by": "pytest", "template_code": "standard_supervision"},
    )

    assert response.status_code == 200

    result = response.json()
    assert result["total_count"] == 1
    assert result["success_count"] == 1
    assert result["failed_count"] == 0
    assert result["batch_id"]

    list_response = client.get("/api/v1/supervision-items")
    assert list_response.status_code == 200
    imported = next(item for item in list_response.json()["items"] if item["title"] == title)
    assert imported["batch_id"] == result["batch_id"]
    assert imported["source_row_no"] == 2
