import pytest
from pydantic import ValidationError

from src.shared.models import ChunkMessage, SegmentMessage


def test_chunk_message_contract():
    message = ChunkMessage(
        job_id="job-1",
        user_id="user-1",
        chunk_index=0,
        s3_key="job-1/chunks/0.mp3",
    )

    assert message.retry_count == 0
    assert message.model_dump()["s3_key"] == "job-1/chunks/0.mp3"


def test_segment_message_contract():
    message = SegmentMessage(b
        job_id="job-1",
        user_id="user-1",
        chunk_index=0,
        seq=0,
        start_ms=0,
        end_ms=1200,
        text="hello world",
    )

    assert message.text == "hello world"


def test_chunk_message_rejects_negative_index():
    with pytest.raises(ValidationError):
        ChunkMessage(
            job_id="job-1",
            user_id="user-1",
            chunk_index=-1,
            s3_key="job-1/chunks/0.mp3",
        )


def test_service_entrypoints_import():
    import src.scheduler.main
    import src.worker.main

    assert src.scheduler.main.main is not None
    assert src.worker.main.main is not None
