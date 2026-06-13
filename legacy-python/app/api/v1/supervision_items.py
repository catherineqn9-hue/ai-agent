from fastapi import APIRouter, HTTPException

from app.core.exceptions import DuplicateResourceError, ResourceNotFoundError
from app.dto.supervision_item_dto import (
    SupervisionItemCreateRequest,
    SupervisionItemResponse,
    SupervisionItemStatusRequest,
    SupervisionItemUpdateRequest,
)
from app.services.supervision_item_service import (
    change_supervision_item_status,
    create_supervision_item,
    delete_supervision_item,
    get_supervision_item,
    list_supervision_items,
    update_supervision_item,
)


router = APIRouter(prefix="/api/v1/supervision-items", tags=["supervision-items"])


@router.get("")
def list_items() -> dict[str, list[SupervisionItemResponse]]:
    return {"items": list_supervision_items()}


@router.post("", response_model=SupervisionItemResponse)
def create_item(request: SupervisionItemCreateRequest) -> dict:
    try:
        return create_supervision_item(request)
    except DuplicateResourceError as exc:
        raise HTTPException(status_code=409, detail=str(exc)) from exc


@router.get("/{item_id}", response_model=SupervisionItemResponse)
def get_item(item_id: str) -> dict:
    try:
        return get_supervision_item(item_id)
    except ResourceNotFoundError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc


@router.put("/{item_id}", response_model=SupervisionItemResponse)
def update_item(item_id: str, request: SupervisionItemUpdateRequest) -> dict:
    try:
        return update_supervision_item(item_id, request)
    except ResourceNotFoundError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    except DuplicateResourceError as exc:
        raise HTTPException(status_code=409, detail=str(exc)) from exc


@router.patch("/{item_id}/status", response_model=SupervisionItemResponse)
def update_item_status(item_id: str, request: SupervisionItemStatusRequest) -> dict:
    try:
        return change_supervision_item_status(item_id, request.status)
    except ResourceNotFoundError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc


@router.delete("/{item_id}")
def delete_item(item_id: str) -> dict[str, str]:
    try:
        delete_supervision_item(item_id)
    except ResourceNotFoundError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    return {"deleted": item_id}
