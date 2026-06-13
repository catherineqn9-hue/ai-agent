from fastapi import APIRouter, HTTPException

from app.agent import service_agents  # noqa: F401 - registers framework agents
from app.agent.framework import get_agent_runtime


router = APIRouter(prefix="/api/v1/framework", tags=["framework"])


@router.get("/config")
def framework_config() -> dict:
    runtime = get_agent_runtime()
    return runtime.describe()


@router.post("/dispatch/{agent_name}")
def framework_dispatch(agent_name: str, payload: dict) -> dict:
    runtime = get_agent_runtime()
    try:
        result = runtime.dispatch(agent_name, payload)
    except KeyError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    return {
        "agent": result.agent,
        "status": result.status,
        "output": result.output,
        "events": result.events,
        "trace": result.trace,
    }
