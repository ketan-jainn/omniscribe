from sqlalchemy.orm import Session
from sqlalchemy.exc import IntegrityError

from src.api.schemas.jobs import JobCreateRequest
from src.shared.db.models import Job, JobStatus
from src.shared.log import get_logger

logger = get_logger(__name__)


def get_job(job_id: str, db: Session) -> Job | None:
    return db.query(Job).filter_by(id=job_id).first()


def create_job(request: JobCreateRequest, db: Session, idempotency_key: str):
    # Check for existing job with the same idempotency key
    logger.info("Creating job", request=request, idempotency_key=idempotency_key)
    if request is not None:
        logger.info("Checking for existing job", idempotency_key=idempotency_key)
        existing_job = db.query(Job).filter_by(idempotency_key=idempotency_key).first()
        if existing_job:
            return existing_job, False
    
    # Create new job
    new_job = Job(
        user_id=request.user_id,
        idempotency_key=idempotency_key,
        status=JobStatus.PENDING_UPLOAD,
    )
    db.add(new_job)
    try:
        db.commit()
        db.refresh(new_job)
        return new_job, True
    except IntegrityError:
        db.rollback()
        existing_job = (
            db.query(Job)
            .filter_by(user_id=request.user_id, idempotency_key=idempotency_key)
            .first()
        )
        return existing_job, False
    except Exception as e:
        db.rollback()
        logger.error("Unexpected error during job creation", error=str(e),
                     user_id=request.user_id, idempotency_key=idempotency_key)
        raise # Re-raise other exceptions to be handled by higher layers (e.g., FastAPI exception handlers)
