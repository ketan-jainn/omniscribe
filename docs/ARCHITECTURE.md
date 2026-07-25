# Omniscribe Architecture

This document describes the architecture, components, data flows, failure modes, and operational runbook for the Omniscribe async transcription platform. It follows the phased roadmap in the project plan (Phase 0 → Phase 4). Use this as the single source of architectural truth for design decisions and future changes.

## One-sentence system summary
Users upload large audio/video files → chunks land in S3 (MinIO locally) → API publishes chunk work to Kafka ingress → a scheduler (passthrough or fair-share) forwards to the jobs topic → workers consume jobs, download chunks from S3, run transcription, publish segments → API streams segments to clients over WebSocket and stores results in Postgres.

---

## High-level components

- Spring Boot 3.4+ / Java 21 Application Platform:
  - `controllers`: HTTP REST endpoints (`POST /v1/jobs`, `GET /v1/jobs/{job_id}`, `/health/live`, `/health/ready`, `/`), validation error handling (`GlobalExceptionHandler`), and Jackson JSON response serialization.
  - `services` / `services.impl`: `JobServiceImpl` (idempotent job management), `SchedulerServiceImpl` (Kafka ingress listener & passthrough forwarder), `WorkerServiceImpl` (Kafka job listener & segment database persistence).
  - `repositories`: Spring Data JPA repositories (`UserRepository`, `JobRepository`, `ChunkRepository`, `SegmentRepository`).
  - `models`: JPA entities (`User`, `Job`, `Chunk`, `Segment`), DTO records (`JobCreateRequest`, `JobResponse`, `HealthResponse`), and Kafka message contracts (`ChunkMessage`, `SegmentMessage`).
  - `messaging`: `KafkaPublisher` component and Spring Kafka message dispatch wrappers.
  - `src/main/resources/db/migration`: Flyway database schema migration scripts (`V1__initial_schema.sql`).
- Infra: PostgreSQL 16 (metadata & persistence), MinIO (S3 object storage), Redpanda (Kafka-compatible message broker), Redis 7 (caching, rate limits, scheduler state), Docker Compose for local dev.

---

## Kafka topics and their purpose

- `transcription.ingress` — messages created by the API for each chunk uploaded. Raw arrival order.
- `transcription.jobs` — work chosen by the scheduler for workers to process (fair interleaving applied here in Phase 3).
- `transcription.segments` — results produced by workers: timestamped text segments for a chunk (consumed by API for streaming/persistence and other consumers).
- `transcription.jobs.retry.30s`, `transcription.jobs.retry.5m` — delayed retry lanes implementing exponential backoff.
- `transcription.dlq` — dead letter queue for permanently failed messages.

Notes: Topics are defined as `NewTopic` beans in Spring Boot (`KafkaConfig.java`) and initialized automatically on startup.

---

## Data flow (end-to-end)

1. Client calls `POST /v1/jobs` with `Idempotency-Key` header. API writes a `jobs` row in Postgres with status `PENDING_UPLOAD`. Returns HTTP 202 Accepted for new job, or HTTP 200 OK for duplicate key.
2. Client uploads file in chunks to S3: `s3://<bucket>/{job_id}/chunks/{n}`. API updates manifest in DB and sets job `UPLOADED` once complete.
3. For each chunk the API publishes a JSON message to `transcription.ingress`:
	```json
	{ "jobId": "...", "userId": "...", "chunkIndex": 0, "s3Key": "...", "retryCount": 0 }
	```
4. Scheduler consumes `transcription.ingress` (`group-id: omniscribe-scheduler`):
	- Phase 1: passthrough publishes the same message to `transcription.jobs`
	- Phase 3: fair-share uses Redis DRR & in-flight caps to decide when to publish to `transcription.jobs`.
5. Workers (`group-id: whisper-workers`) consume `transcription.jobs`, download chunk from S3, run transcription, publish `transcription.segments` and write segment rows to Postgres (idempotent write: UNIQUE(job_id, chunk_index, seq)).
6. API streams incoming segments over WebSocket `ws://.../v1/jobs/{id}/stream` to connected clients or replays from Postgres.
7. When all chunks are processed, job status becomes `COMPLETED`; if a chunk repeatedly fails it moves to DLQ and job status becomes `FAILED`.

---

## Job lifecycle and states

- `PENDING_UPLOAD` — job created, awaiting client uploads
- `UPLOADED` — manifest complete in Postgres
- `QUEUED` — chunk messages published to `transcription.ingress`
- `PROCESSING` — worker is actively transcribing chunk(s)
- `COMPLETED` — all chunks processed and segments persisted
- `FAILED` — fatal error preventing completion
- `DLQ` — chunk message moved to dead-letter queue after retries

---

## Postgres schema & Flyway migrations

Schema migrations are managed by Flyway (`src/main/resources/db/migration/`):

- `users`: `id` (PK), `plan`, `rate_limit_tier`, `created_at`, `updated_at`
- `jobs`: `id` (PK), `user_id`, `status`, `s3_prefix`, `chunk_count`, `idempotency_key` (UNIQUE), `created_at`, `updated_at`
- `chunks`: `id` (PK), `job_id` (FK), `"index"`, `s3_key`, `status`, `retry_count`, `created_at`, `updated_at` — UNIQUE(`job_id`, `"index"`)
- `segments`: `id` (PK), `job_id` (FK), `chunk_index`, `seq`, `start_ms`, `end_ms`, `text`, `created_at` — UNIQUE(`job_id`, `chunk_index`, `seq`)

---

## Reliability: retries, DLQ, idempotency

- Idempotency keys on `POST /v1/jobs` and DB unique constraints on `idempotency_key` guarantee single job creation per key.
- Unique constraints on `chunks` (`job_id, index`) and `segments` (`job_id, chunk_index, seq`) prevent duplicate rows upon Kafka re-delivery.
- Global exception handling converts validation errors to HTTP 422 with structured detail responses.

---

## Scheduler: passthrough vs fair-share

- Phase 1: scheduler is a lightweight passthrough consumer/producer. Simple, minimal logic, used to validate pipeline.
- Phase 3: scheduler implements a Deficit Round Robin (DRR) or weighted fair queuing using Redis:
  - Maintain per-user queues and deficit counters.
  - Compute job cost (chunks or approximate MB/10MB).
  - Cap per-user in-flight chunks with Redis semaphores.
  - Publish from per-user queues to `transcription.jobs` in fair order.

---

## Observability & health

- Health endpoints (API):
  - `/health/live` — Liveness check (`{"status": "ok", "db": "ok"}`)
  - `/health/ready` — Readiness check verifying database connectivity
  - `/` — Service index (`{"service": "omniscribe api", "status": "ok"}`)
  - `/actuator/health` — Spring Boot Actuator health endpoint
- Metrics & Logging: Structured SLF4J / Logback logging with `job_id` and `user_id` MDC context.

---

## Deployment & Local Development

### Local Docker Compose
Start infrastructure and Spring Boot containerized services:
```bash
docker compose up -d
```

### Maven Commands
```bash
# Run integration & unit test suite (H2 in-memory DB)
mvn clean test

# Build executable JAR
mvn clean package -DskipTests

# Run Spring Boot service locally
mvn spring-boot:run
```

---

## Extensibility and Phase Boundaries

- **Separation of Services**: API, Scheduler, and Worker components can be launched independently using configuration toggles (`OMNISCRIBE_SCHEDULER_ENABLED`, `OMNISCRIBE_WORKER_ENABLED`).
- Phase changes:
  - Phase 1: passthrough scheduler, core pipeline working end-to-end.
  - Phase 2: add retry topics, DLQ, idempotent writes.
  - Phase 3: replace passthrough scheduler with Redis DRR fair-share implementation.
  - Phase 4: Terraform + AWS ECS + monitoring and production hardening.