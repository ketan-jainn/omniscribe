# Learning Notes: Omniscribe Platform

- **Architecture Overview**: API, Scheduler (passthrough → fair-share), Worker (transcription & segment persistence), Shared (models + kafka topics), Infra (Postgres 16, Redis 7, MinIO, Redpanda).
- **Spring Boot Platform (Java 21)**:
  - Spring Boot 3.4.1 workspace at root with Maven parent build (`pom.xml`).
  - Flyway manages database migrations (`V1__initial_schema.sql`).
  - JPA Entities (`User`, `Job`, `Chunk`, `Segment`) map database tables with constraints (`UNIQUE(job_id, idempotency_key)`, `UNIQUE(job_id, index)`, `UNIQUE(job_id, chunk_index, seq)`).
  - REST Endpoints: `POST /v1/jobs` (202 Accepted / 200 OK idempotency), `GET /v1/jobs/{job_id}` (200 / 404), `/health/live`, `/health/ready`, `/`.
  - Validation errors mapped to HTTP 422 `UNPROCESSABLE_ENTITY` via `GlobalExceptionHandler` with standard Java `camelCase` field naming (`jobId`, `userId`, `createdAt`, `idempotencyKey`).
  - Spring Kafka messaging configured for `transcription.ingress`, `transcription.jobs`, `transcription.segments`, retry topics, and DLQ.
  - Containerized deployment with Docker Compose (`deploy/docker-compose.yml`) supporting API, Scheduler, and Worker.
