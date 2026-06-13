from fastapi import APIRouter
from fastapi.responses import FileResponse


router = APIRouter(tags=["pages"])


@router.get("/demo", include_in_schema=False)
def demo_page() -> FileResponse:
    return FileResponse("static/demo.html")


@router.get("/", include_in_schema=False)
def index_page() -> FileResponse:
    return FileResponse("static/admin.html")


@router.get("/admin", include_in_schema=False)
def admin_page() -> FileResponse:
    return FileResponse("static/admin.html")
