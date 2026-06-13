from pydantic import BaseModel


class SupervisionImportResponse(BaseModel):
    batch_id: str
    batch_no: str
    batch_name: str
    import_status: str
    total_count: int
    success_count: int
    failed_count: int
    errors: list[str]
