from fastapi import FastAPI
from fastapi.staticfiles import StaticFiles

from app.api.v1 import (
    admin_menus,
    basic_configs,
    chat,
    framework,
    legacy_configs,
    pages,
    progress_feedbacks,
    supervision_imports,
    supervision_items,
)


def create_app() -> FastAPI:
    app = FastAPI(
        title="Sherry Dialogue Agent MVP",
        description="FastAPI + LangGraph task-oriented dialogue agent MVP",
        version="0.1.0",
    )

    app.mount("/static", StaticFiles(directory="static"), name="static")

    @app.get("/health")
    def health() -> dict[str, str]:
        return {"status": "ok"}

    app.include_router(pages.router)
    app.include_router(framework.router)
    app.include_router(chat.router)
    app.include_router(legacy_configs.router)
    app.include_router(basic_configs.router)
    app.include_router(admin_menus.router)
    app.include_router(supervision_imports.router)
    app.include_router(supervision_items.router)
    app.include_router(progress_feedbacks.router)
    return app


app = create_app()
