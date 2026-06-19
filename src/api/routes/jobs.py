from fastapi import APIRouter, Depends, Response, status
from sqlalchemy.orm import Session

from src.api.schemas.jobs import JobCreateRequest, JobResponse
from src.services.jobs import create_job
from src.shared.db.base import get_db
from src.shared.log import get_logger

logger = get_logger(__name__)
router = APIRouter()


@router.post("/v1/jobs", response_model=JobResponse)
def post_job(
    req: JobCreateRequest,
    response: Response,
    db: Session = Depends(get_db),
):
    logger.info("Creating job", user_id=req.user_id)

    job, created = create_job(req, db)

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