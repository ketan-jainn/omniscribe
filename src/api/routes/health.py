from fastapi import APIRouter, Depends
from sqlalchemy import text
from sqlalchemy.exc import OperationalError
from sqlalchemy.orm import Session

from src.shared.db.base import get_db
from src.shared.log import get_logger
from src.shared.config import settings

logger = get_logger(__name__)
router = APIRouter()


@router.get("/live")
async def live():
    return {"status": "ok"}


@router.get("/ready")
async def ready(db: Session = Depends(get_db)):
    database_url = settings.DATABASE_URL
    logger.info("Performing readiness check", db=database_url)

    if not database_url:
        logger.warning("Database URL is not configured")
        return {"status": "ok", "db": "not-configured"}

    try:
        db.execute(text("SELECT 1"))
        return {"status": "ok", "db": "ok"}
    except OperationalError:
        logger.error("Database is unreachable")
        return {"status": "error", "db": "unreachable"}
    except Exception as exc:
        logger.exception("Unexpected database error")
        return {"status": "error", "db": "error", "detail": str(exc)}