from fastapi import APIRouter, File, Form, HTTPException, UploadFile

from app.dto.supervision_import_dto import SupervisionImportResponse
from app.services.supervision_import_service import (
    DEFAULT_TEMPLATE_CODE,
    import_supervision_excel,
    list_import_templates,
)


router = APIRouter(prefix="/api/v1/supervision-imports", tags=["supervision-imports"])


@router.get("/templates")
def import_templates() -> dict:
    return {"templates": list_import_templates()}


@router.post("/excel", response_model=SupervisionImportResponse)
async def import_excel(
    file: UploadFile = File(...),
    created_by: str = Form(default="admin"),
    template_code: str = Form(default=DEFAULT_TEMPLATE_CODE),
) -> dict:
    if not file.filename or not file.filename.endswith((".xlsx", ".xlsm")):
        raise HTTPException(status_code=400, detail="only .xlsx or .xlsm files are supported")

    content = await file.read()
    if not content:
        raise HTTPException(status_code=400, detail="empty file")

    return import_supervision_excel(
        filename=file.filename,
        content=content,
        created_by=created_by,
        template_code=template_code,
    )
