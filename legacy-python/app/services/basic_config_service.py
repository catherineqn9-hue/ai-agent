from typing import Any

from app.db import get_connection
from app.repositories.basic_config_repository import (
    CONFIG_RESOURCES,
    create_config,
    delete_config,
    get_config,
    list_configs,
    update_config,
)


def describe_resources() -> list[dict[str, Any]]:
    return [
        {
            "name": resource.name,
            "table": resource.table,
            "key_field": resource.key_field,
            "name_field": resource.name_field,
            "fields": list(resource.list_fields),
            "json_fields": list(resource.json_fields),
            "bool_fields": list(resource.bool_fields),
        }
        for resource in CONFIG_RESOURCES.values()
    ]


def list_resource_configs(resource_name: str) -> list[dict[str, Any]]:
    with get_connection() as conn:
        return list_configs(conn, resource_name)


def create_resource_config(resource_name: str, payload: dict[str, Any]) -> dict[str, Any]:
    with get_connection() as conn:
        return create_config(conn, resource_name, payload)


def get_resource_config(resource_name: str, item_id: str) -> dict[str, Any]:
    with get_connection() as conn:
        return get_config(conn, resource_name, item_id)


def update_resource_config(
    resource_name: str,
    item_id: str,
    payload: dict[str, Any],
) -> dict[str, Any]:
    with get_connection() as conn:
        return update_config(conn, resource_name, item_id, payload)


def delete_resource_config(resource_name: str, item_id: str) -> None:
    with get_connection() as conn:
        delete_config(conn, resource_name, item_id)
