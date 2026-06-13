from fastapi import APIRouter, HTTPException

from app.schemas import (
    BusinessConfigCreate,
    BusinessConfigItem,
    BusinessConfigListResponse,
    BusinessConfigUpdate,
)
from app.storage import business_config_store


router = APIRouter(prefix="/api/v1/configs", tags=["legacy-configs"])


@router.get("", response_model=BusinessConfigListResponse)
def list_business_configs(config_type: str | None = None) -> BusinessConfigListResponse:
    return BusinessConfigListResponse(items=business_config_store.list(config_type))


@router.post("", response_model=BusinessConfigItem)
def create_business_config(request: BusinessConfigCreate) -> BusinessConfigItem:
    try:
        item = business_config_store.create(request.model_dump())
    except ValueError as exc:
        raise HTTPException(status_code=409, detail=str(exc)) from exc
    return BusinessConfigItem.model_validate(item)


@router.get("/{item_id}", response_model=BusinessConfigItem)
def get_business_config(item_id: str) -> BusinessConfigItem:
    item = business_config_store.get(item_id)
    if item is None:
        raise HTTPException(status_code=404, detail="config not found")
    return BusinessConfigItem.model_validate(item)


@router.put("/{item_id}", response_model=BusinessConfigItem)
def update_business_config(
    item_id: str,
    request: BusinessConfigUpdate,
) -> BusinessConfigItem:
    try:
        item = business_config_store.update(item_id, request.model_dump(exclude_unset=True))
    except ValueError as exc:
        raise HTTPException(status_code=409, detail=str(exc)) from exc
    if item is None:
        raise HTTPException(status_code=404, detail="config not found")
    return BusinessConfigItem.model_validate(item)


@router.delete("/{item_id}")
def delete_business_config(item_id: str) -> dict[str, str]:
    if not business_config_store.delete(item_id):
        raise HTTPException(status_code=404, detail="config not found")
    return {"deleted": item_id}
