class AppError(Exception):
    """Base application error."""


class ResourceNotFoundError(AppError):
    """Requested resource does not exist."""


class DuplicateResourceError(AppError):
    """Resource unique key already exists."""
