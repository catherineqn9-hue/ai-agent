from app.core.exceptions import DuplicateResourceError, ResourceNotFoundError
from app.repositories.basic_config_repository import (
    CONFIG_RESOURCES,
    ConfigResource,
    create_config,
    delete_config,
    get_config,
    get_resource,
    list_configs,
    update_config,
)


ConfigNotFoundError = ResourceNotFoundError
DuplicateConfigError = DuplicateResourceError

__all__ = [
    "CONFIG_RESOURCES",
    "ConfigResource",
    "ConfigNotFoundError",
    "DuplicateConfigError",
    "create_config",
    "delete_config",
    "get_config",
    "get_resource",
    "list_configs",
    "update_config",
]
