from fastapi import APIRouter, HTTPException

from app.core.exceptions import DuplicateResourceError, ResourceNotFoundError
from app.services.basic_config_service import (
    create_resource_config,
    delete_resource_config,
    describe_resources,
    get_resource_config,
    list_resource_configs,
    update_resource_config,
)


router = APIRouter(prefix="/api/v1/basic-configs", tags=["basic-configs"])


@router.get("/resources")
def basic_config_resources() -> dict:
    return {"resources": describe_resources()}


@router.get("/{resource_name}")
def list_basic_configs(resource_name: str) -> dict:
    try:
        return {"items": list_resource_configs(resource_name)}
    except ResourceNotFoundError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc


@router.post("/{resource_name}")
def create_basic_config(resource_name: str, payload: dict) -> dict:
    try:
        return create_resource_config(resource_name, payload)
    except ResourceNotFoundError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    except DuplicateResourceError as exc:
        raise HTTPException(status_code=409, detail=str(exc)) from exc


@router.get("/{resource_name}/{item_id}")
def get_basic_config(resource_name: str, item_id: str) -> dict:
    try:
        return get_resource_config(resource_name, item_id)
    except ResourceNotFoundError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc


@router.put("/{resource_name}/{item_id}")
def update_basic_config(resource_name: str, item_id: str, payload: dict) -> dict:
    try:
        return update_resource_config(resource_name, item_id, payload)
    except ResourceNotFoundError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    except DuplicateResourceError as exc:
        raise HTTPException(status_code=409, detail=str(exc)) from exc


@router.delete("/{resource_name}/{item_id}")
def delete_basic_config(resource_name: str, item_id: str) -> dict[str, str]:
    try:
        delete_resource_config(resource_name, item_id)
    except ResourceNotFoundError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    return {"deleted": item_id}
