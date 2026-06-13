from copy import deepcopy
from threading import Lock
from typing import Any
from uuid import uuid4


class InMemoryConversationStore:
    def __init__(self) -> None:
        self._lock = Lock()
        self._states: dict[str, dict[str, Any]] = {}

    def get(self, thread_id: str) -> dict[str, Any] | None:
        with self._lock:
            state = self._states.get(thread_id)
            return deepcopy(state) if state else None

    def save(self, thread_id: str, state: dict[str, Any]) -> None:
        with self._lock:
            self._states[thread_id] = deepcopy(state)

    def messages(self, thread_id: str) -> list[dict[str, str]]:
        state = self.get(thread_id) or {}
        return state.get("messages", [])

    def trace(self, thread_id: str) -> list[dict[str, str]]:
        state = self.get(thread_id) or {}
        return state.get("trace", [])


conversation_store = InMemoryConversationStore()


class InMemoryBusinessConfigStore:
    def __init__(self) -> None:
        self._lock = Lock()
        self._items: dict[str, dict[str, Any]] = {}
        self._seed()

    def _seed(self) -> None:
        for item in [
            {
                "config_type": "agent",
                "key": "task_parser",
                "name": "Task Parser Agent",
                "description": "Parse supervision Excel or text into events.",
                "enabled": True,
                "data": {"provider": "langchain", "role": "task_parser"},
            },
            {
                "config_type": "user_service",
                "key": "default_directory",
                "name": "Default User Directory",
                "description": "User matching adapter placeholder.",
                "enabled": False,
                "data": {"base_url": "", "match_fields": ["name", "department"]},
            },
            {
                "config_type": "im_template",
                "key": "due_reminder",
                "name": "Due Reminder",
                "description": "Template for due-date reminder messages.",
                "enabled": True,
                "data": {"channel": "im", "template": "Please update progress for {task}."},
            },
            {
                "config_type": "status",
                "key": "in_progress",
                "name": "In Progress",
                "description": "Event is being handled by the owner.",
                "enabled": True,
                "data": {"terminal": False},
            },
        ]:
            item_id = str(uuid4())
            self._items[item_id] = {"id": item_id, **item}

    def list(self, config_type: str | None = None) -> list[dict[str, Any]]:
        with self._lock:
            values = list(self._items.values())
            if config_type:
                values = [item for item in values if item["config_type"] == config_type]
            return deepcopy(sorted(values, key=lambda item: (item["config_type"], item["key"])))

    def get(self, item_id: str) -> dict[str, Any] | None:
        with self._lock:
            item = self._items.get(item_id)
            return deepcopy(item) if item else None

    def create(self, item: dict[str, Any]) -> dict[str, Any]:
        with self._lock:
            self._ensure_unique_key(item["config_type"], item["key"])
            item_id = str(uuid4())
            stored = {"id": item_id, **deepcopy(item)}
            self._items[item_id] = stored
            return deepcopy(stored)

    def update(self, item_id: str, patch: dict[str, Any]) -> dict[str, Any] | None:
        with self._lock:
            current = self._items.get(item_id)
            if current is None:
                return None
            next_key = patch.get("key", current["key"])
            self._ensure_unique_key(current["config_type"], next_key, exclude_id=item_id)
            updated = {**current, **{k: v for k, v in patch.items() if v is not None}}
            self._items[item_id] = updated
            return deepcopy(updated)

    def delete(self, item_id: str) -> bool:
        with self._lock:
            return self._items.pop(item_id, None) is not None

    def _ensure_unique_key(
        self,
        config_type: str,
        key: str,
        exclude_id: str | None = None,
    ) -> None:
        for item_id, item in self._items.items():
            if item_id == exclude_id:
                continue
            if item["config_type"] == config_type and item["key"] == key:
                raise ValueError(f"Duplicate config key: {config_type}/{key}")


business_config_store = InMemoryBusinessConfigStore()
