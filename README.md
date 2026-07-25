# Omniscribe

Omniscribe is an async transcription platform for large audio and video files built with Java 21 and Spring Boot.

- Clients create jobs through the API (`POST /v1/jobs` with `Idempotency-Key` header)
- Files are stored in S3-compatible object storage (MinIO locally)
- Work is queued through Kafka topics (`transcription.ingress`, `transcription.jobs`, `transcription.segments`)
- Workers transcribe chunks and persist segments to PostgreSQL
- The API exposes health checks (`/health/live`, `/health/ready`) and job lookup (`GET /v1/jobs/{job_id}`)

---

## Service Architecture

The repository is structured as a Spring Boot 3.4+ Java platform with explicit service layers and separate runtime execution capabilities for API, Scheduler, and Worker.

```
omniscribe/
├── deploy/                       # DevOps & Container Infrastructure Configuration
│   ├── Dockerfile                # Multi-stage Container Build
│   └── docker-compose.yml        # Full Stack System Orchestration
├── pom.xml                       # Maven Project Configuration
├── src/
│   ├── main/
│   │   ├── java/com/omniscribe/
│   │   │   ├── config/           # Kafka, CORS, Web configuration
│   │   │   ├── controllers/      # JobController, HealthController, RootController, GlobalExceptionHandler
│   │   │   ├── dto/              # Business Layer DTOs (UserDto, JobDto, ChunkDto, SegmentDto)
│   │   │   ├── mappers/          # MapStruct Mappers (UserMapper, JobMapper, ChunkMapper, SegmentMapper)
│   │   │   ├── models/           # Entities (User, Job, Chunk, Segment), DTO records, Message Contracts
│   │   │   ├── repositories/     # UserRepository, JobRepository, ChunkRepository, SegmentRepository
│   │   │   ├── services/         # JobService, SchedulerService, WorkerService
│   │   │   ├── services/impl/    # JobServiceImpl, SchedulerServiceImpl, WorkerServiceImpl
│   │   │   └── messaging/        # KafkaPublisher
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/     # Flyway SQL Schema Migrations (V1__initial_schema.sql)
│   └── test/                     # Integration & Unit Tests (H2 / MockMvc)
├── docs/                         # System Architecture documentation & Handover notes
└── README.md
```

---

## Single Command Execution

To launch the **entire system** (PostgreSQL, Redis, MinIO, Redpanda Kafka, Spring Boot API, Scheduler, and Worker services):

```bash
docker compose -f deploy/docker-compose.yml up --build -d
```
*Alternatively from the workspace root: `docker compose up --build -d`*

Services started:
- **Spring Boot API**: `http://localhost:8080`
- **Spring Boot Scheduler**: `http://localhost:8081`
- **Spring Boot Worker**: `http://localhost:8082`
- **PostgreSQL**: `localhost:5432` (`omniscribe_db`)
- **Redis**: `localhost:6379`
- **MinIO S3**: `localhost:9000` (Console: `localhost:9002`)
- **Redpanda Kafka**: `localhost:9092`

---

## Local Maven Development

### Run Integration Tests
```bash
mvn clean test
```

### Build Executable JAR
```bash
mvn clean package -DskipTests
```

### Run API Server Locally
```bash
mvn spring-boot:run
```

---

## API Endpoints & Specifications

| Method | Path | Request Headers | Description | Status Codes |
|---|---|---|---|---|
| `GET` | `/` | - | Root index endpoint | `200 OK` |
| `GET` | `/health/live` | - | Liveness health check | `200 OK` |
| `GET` | `/health/ready` | - | Readiness check (verifies PostgreSQL connection) | `200 OK` / `503 Service Unavailable` |
| `POST` | `/v1/jobs` | `Idempotency-Key: <key>` | Idempotent job creation | `202 Accepted` (new), `200 OK` (duplicate key), `422 Unprocessable Entity` (missing header/fields) |
| `GET` | `/v1/jobs/{job_id}` | - | Job details lookup | `200 OK`, `404 Not Found` (`{"detail": "Job not found"}`) |

---

## Kafka Messaging Topics

- `transcription.ingress`: Initial chunk job messages created by API.
- `transcription.jobs`: Scheduled job work consumed by worker pool.
- `transcription.segments`: Transcribed segment output published by worker.
- `transcription.jobs.retry.30s`: Delayed 30-second retry queue.
- `transcription.jobs.retry.5m`: Delayed 5-minute retry queue.
- `transcription.dlq`: Dead Letter Queue for failed job events.
