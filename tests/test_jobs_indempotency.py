import pytest
from fastapi import status
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import Session, sessionmaker
from sqlalchemy.pool import StaticPool

from src.api.main import app # Import your main FastAPI app
from src.shared.db.base import Base, get_db # Import your Base and get_db
from src.shared.db.models import Job, JobStatus # Import your Job model
from src.api.schemas.jobs import JobCreateRequest # Import your request schema

# --- Database setup for testing ---
# Use an in-memory SQLite database for fast, isolated tests.
SQLALCHEMY_DATABASE_URL = "sqlite://"
engine = create_engine(
    SQLALCHEMY_DATABASE_URL,
    connect_args={"check_same_thread": False},
    poolclass=StaticPool,
)
TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

# Override the get_db dependency to use the test database
def override_get_db():
    try:
        db = TestingSessionLocal()
        yield db
    finally:
        db.close()

app.dependency_overrides[get_db] = override_get_db

# --- Test client setup ---
client = TestClient(app)

@pytest.fixture(name="db_session")
def db_session_fixture():
    # Create the database tables before each test
    Base.metadata.create_all(bind=engine)
    db = TestingSessionLocal()
    try:
        yield db
    finally:
        db.close()
    # Drop the database tables after each test
    Base.metadata.drop_all(bind=engine)

# --- Actual Tests ---
def test_create_new_job(db_session: Session):
    """
    Test that a new job is created successfully.
    """
    user_id = "test_user_123"
    idempotency_key = "test_key_abc"
    payload = JobCreateRequest(
        user_id=user_id,
        idempotency_key=idempotency_key,
        chunks=["s3://test-bucket/chunk1.mp3"]
    ).model_dump()

    response = client.post("/v1/jobs", json=payload)

    assert response.status_code == status.HTTP_202_ACCEPTED
    data = response.json()
    assert data["user_id"] == user_id
    assert data["idempotency_key"] == idempotency_key
    assert data["status"] == JobStatus.PENDING_UPLOAD.value
    assert "job_id" in data

    # Verify job in DB
    job_in_db = db_session.query(Job).filter_by(id=data["job_id"]).first()
    assert job_in_db is not None
    assert job_in_db.user_id == user_id
    assert job_in_db.idempotency_key == idempotency_key

def test_idempotent_job_creation(db_session: Session):
    """
    Test that sending the same request twice results in the same job and a 200 OK.
    """
    user_id = "test_user_456"
    idempotency_key = "test_key_def"
    payload = JobCreateRequest(
        user_id=user_id,
        idempotency_key=idempotency_key,
        chunks=["s3://test-bucket/chunk2.mp3"]
    ).model_dump()

    # First request: should create a new job (202 Accepted)
    response1 = client.post("/v1/jobs", json=payload)
    assert response1.status_code == status.HTTP_202_ACCEPTED
    data1 = response1.json()
    job_id1 = data1["job_id"]

    # Second request with the same idempotency key: should return the existing job (200 OK)
    response2 = client.post("/v1/jobs", json=payload)
    assert response2.status_code == status.HTTP_200_OK
    data2 = response2.json()
    job_id2 = data2["job_id"]

    # Verify that the same job ID is returned
    assert job_id1 == job_id2
    assert data2["status"] == JobStatus.PENDING_UPLOAD.value

    # Verify only one job exists in the database for this user/idempotency_key
    jobs_in_db = db_session.query(Job).filter_by(user_id=user_id, idempotency_key=idempotency_key).all()
    assert len(jobs_in_db) == 1
    assert jobs_in_db[0].id == job_id1


def test_get_job_by_id(db_session: Session):
    user_id = "test_user_get"
    idempotency_key = "test_key_get"
    payload = JobCreateRequest(
        user_id=user_id,
        idempotency_key=idempotency_key,
        chunks=["s3://test-bucket/chunk-get.mp3"],
    ).model_dump()

    create_response = client.post("/v1/jobs", json=payload)
    assert create_response.status_code == status.HTTP_202_ACCEPTED
    job_id = create_response.json()["job_id"]

    response = client.get(f"/v1/jobs/{job_id}")

    assert response.status_code == status.HTTP_200_OK
    data = response.json()
    assert data["job_id"] == job_id
    assert data["user_id"] == user_id
    assert data["idempotency_key"] == idempotency_key


def test_get_job_by_id_returns_404_for_missing_job(db_session: Session):
    response = client.get("/v1/jobs/missing-job-id")

    assert response.status_code == status.HTTP_404_NOT_FOUND
    assert response.json() == {"detail": "Job not found"}

def test_create_job_with_missing_required_field():
    """
    Test that missing required fields result in a 422 Unprocessable Entity.
    """
    payload = {
        "idempotency_key": "bad_request_key",
        "chunks": ["s3://test-bucket/chunk3.mp3"]
        # user_id is missing
    }
    response = client.post("/v1/jobs", json=payload)
    assert response.status_code == status.HTTP_422_UNPROCESSABLE_CONTENT
    assert "detail" in response.json()
    assert any("user_id" in error["loc"] for error in response.json()["detail"])

def test_create_job_with_invalid_chunks_list():
    """
    Test that invalid chunks list (e.g., empty) results in a 422 Unprocessable Entity.
    """
    payload = {
        "user_id": "test_user_789",
        "idempotency_key": "invalid_chunks_key",
        "chunks": [],
    }

    response = client.post("/v1/jobs", json=payload)
    assert response.status_code == status.HTTP_422_UNPROCESSABLE_CONTENT
    assert "detail" in response.json()
    assert any("chunks" in error["loc"] for error in response.json()["detail"])
