from fastapi import APIRouter

from app.services.admin_menu_service import list_admin_menus


router = APIRouter(prefix="/api/v1/admin-menus", tags=["admin-menus"])


@router.get("")
def get_admin_menus() -> dict:
    return {"menus": list_admin_menus()}
