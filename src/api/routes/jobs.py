from typing import Annotated
from fastapi import APIRouter, Depends, HTTPException, Header, Response, status
from sqlalchemy.orm import Session

from src.api.schemas.jobs import JobCreateRequest, JobResponse
from src.services.jobs import create_job, get_job
from src.shared.db.base import get_db
from src.shared.log import get_logger

logger = get_logger(__name__)
router = APIRouter()


@router.post("/v1/jobs", response_model=JobResponse)
def post_job(
    req: JobCreateRequest,
    response: Response,
    idempotency_key: Annotated[
        str,
        Header(alias="Idempotency-Key", min_length=1, max_length=255),
    ],
    db: Session = Depends(get_db),
):
    logger.info("Creating job", user_id=req.user_id, idempotency_key=idempotency_key)

    job, created = create_job(req, db, idempotency_key)

    response.status_code = (
        status.HTTP_202_ACCEPTED if created else status.HTTP_200_OK
    )

    return JobResponse(
        job_id=job.id,
        status=job.status.value if hasattr(job.status, "value") else str(job.status),
        user_id=job.user_id,
        created_at=job.created_at,
        idempotency_key=job.idempotency_key,
    )


@router.get("/v1/jobs/{job_id}", response_model=JobResponse)
def get_job_by_id(job_id: str, db: Session = Depends(get_db)):
    job = get_job(job_id, db)
    if job is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Job not found",
        )

    return JobResponse(
        job_id=job.id,
        status=job.status.value if hasattr(job.status, "value") else str(job.status),
        user_id=job.user_id,
        created_at=job.created_at,
        idempotency_key=job.idempotency_key,
    )
