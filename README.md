# Omniscribe

Omniscribe is an async transcription platform for large audio and video files. The intended flow is:

- Clients create jobs through the API
- Files are stored in S3-compatible object storage
- Work is queued through Kafka-compatible topics
- Workers transcribe chunks and persist segments
- The API exposes health checks and, later, live transcript streaming

## Overview

This repository is structured as a Python backend platform with separate pieces for API, background processing, and shared infrastructure code.

The current codebase includes:

- FastAPI application entrypoint
- Health check endpoints
- Job creation and lookup endpoints
- SQLAlchemy models and session setup
- Alembic migrations
- Shared Kafka message contracts and thin producer/consumer wrappers
- Runnable scheduler and worker entrypoints
- Kafka topic bootstrap script
- Local Docker Compose stack for Postgres, Redis, MinIO, and Redpanda

## Tech Stack

- Python 3.11+
- FastAPI
- SQLAlchemy
- Alembic
- PostgreSQL
- Redis
- Kafka / Redpanda
- MinIO
- Pydantic Settings
- structlog
- uvicorn

## Project Layout

- `src/api` - FastAPI app and HTTP routes
- `src/shared` - shared config, logging, database, and Kafka helpers
- `src/scheduler` - message scheduling logic
- `src/worker` - transcription worker logic
- `migrations` - Alembic migrations
- `deploy` - Docker Compose for local infrastructure
- `scripts` - utility scripts such as Kafka topic creation
- `tests` - automated tests
- `docs` - architecture and planning notes

## Local Development

### 1. Install dependencies

This project is managed with modern Python packaging. Use your preferred environment tool, then install dependencies from the project metadata.

### 2. Configure environment variables

Copy the example environment file and adjust values if needed.

Required values are documented in the example file and include:

- `DATABASE_URL`
- `KAFKA_BOOTSTRAP_SERVERS`
- `S3_ENDPOINT`
- `S3_BUCKET`
- `S3_ACCESS_KEY`
- `S3_SECRET_KEY`
- `REDIS_URL`
- `LOG_LEVEL`

### 3. Start local infrastructure

Use Docker Compose to start the supporting services:

```bash
docker compose -f deploy/docker-compose.yml up -d
```

This starts:

- PostgreSQL on port 5432
- Redis on port 6379
- MinIO on ports 9000 and 9001
- Redpanda on port 9092

### 4. Apply database migrations

```bash
alembic upgrade head
```

### 5. Create Kafka topics

```bash
python -m scripts.create_topics
```

### 6. Run the API

```bash
uvicorn src.api.main:app --reload --port 8000
```

### 7. Run Phase 0 service shells

These processes start and subscribe to their Kafka topics. Passthrough scheduling and transcription processing are added in Phase 1.

```bash
python -m src.scheduler.main
python -m src.worker.main
```

## Health Checks

The API currently exposes:

- `GET /` - basic service status
- `GET /health/live` - liveness check
- `GET /health/ready` - readiness check that verifies database connectivity
- `POST /v1/jobs` - create a transcription job
- `GET /v1/jobs/{job_id}` - fetch job metadata

## Database Models

The main data model currently includes:

- `users` - user identity and future rate-limit tier data
- `jobs` - transcription job metadata
- `chunks` - chunk-level upload and processing state
- `segments` - transcript segments produced by workers

## Kafka Topics

The shared topic names are:

- `transcription.ingress`
- `transcription.jobs`
- `transcription.segments`
- `transcription.jobs.retry.30s`
- `transcription.jobs.retry.5m`
- `transcription.dlq`

## Testing

Run the current test suite with:

```bash
pytest
```

## Architecture Notes

The longer-term architecture is documented in the repo’s architecture notes and roadmap. The intended service split is:

- API for job creation, readiness, and transcript delivery
- Scheduler for moving work from ingress into execution
- Worker for transcription and persistence
- Shared contracts for models, topic names, and infrastructure helpers

## Status

This project is under active development. Phase 0 foundation is in place: local infrastructure, database schema, health checks, job metadata APIs, shared Kafka contracts, topic creation, and runnable scheduler/worker shells. Phase 1 will add upload handling, passthrough scheduling, and real transcription processing.
