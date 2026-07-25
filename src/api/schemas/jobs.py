from typing import Annotated, Optional, Dict
from pydantic import BaseModel, Field, AnyUrl
from datetime import datetime


UserId = Annotated[str, Field(min_length=1, max_length=128)]
S3Key = Annotated[str, Field(min_length=1, max_length=1024)]

class JobCreateRequest(BaseModel):
    user_id: UserId
    chunks: list[S3Key] = Field(min_length=1)
    metadata: Optional[Dict[str, str]] = None
    callback_url: Optional[AnyUrl] = None
    priority: Optional[int] = Field(0, ge=0, le=100)

class JobResponse(BaseModel):
    job_id: str
    status: str
    user_id: str
    created_at: datetime
    idempotency_key: str
