from typing import Annotated

from pydantic import BaseModel, Field


JobId = Annotated[str, Field(min_length=1, max_length=36)]
UserId = Annotated[str, Field(min_length=1, max_length=128)]
S3Key = Annotated[str, Field(min_length=1, max_length=1024)]


class ChunkMessage(BaseModel):
    job_id: JobId
    user_id: UserId
    chunk_index: int = Field(ge=0)
    s3_key: S3Key
    retry_count: int = Field(default=0, ge=0)


class SegmentMessage(BaseModel):
    job_id: JobId
    user_id: UserId
    chunk_index: int = Field(ge=0)
    seq: int = Field(ge=0)
    start_ms: int | None = Field(default=None, ge=0)
    end_ms: int | None = Field(default=None, ge=0)
    text: str = Field(min_length=1)
