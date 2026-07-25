# AI Handover: Omniscribe Platform

## Purpose

This document is the resume point for any AI continuing development on the Omniscribe platform.

## Current State

- The backend implementation is Spring Boot 3.4.1 using Maven and Java 21 at the workspace root.
- Runtime services: API, Scheduler, and Worker.
- Infrastructure stack: Postgres 16, Kafka (Redpanda), Redis 7, and MinIO.
- Package layout under `com.omniscribe`: `controllers`, `services`, `services.impl`, `repositories`, `models`, `config`, `messaging`.
- Strong static typing required across all entities, DTOs, and services.

## System Overview & Completed Work

- [x] Maven project setup with Spring Boot 3.4.1 and Java 21 dependencies (`pom.xml`).
- [x] Database Flyway migration script (`V1__initial_schema.sql`) and JPA entities (`User`, `Job`, `Chunk`, `Segment`).
- [x] HTTP REST endpoints (`POST /v1/jobs`, `GET /v1/jobs/{job_id}`, `/health/live`, `/health/ready`, `/`).
- [x] Global exception handling mapping validation errors to HTTP 422 `UNPROCESSABLE_ENTITY`.
- [x] Kafka producer (`KafkaPublisher`), topic definitions (`KafkaTopics`, `KafkaConfig`), passthrough scheduler (`SchedulerServiceImpl`), and worker segment persistence (`WorkerServiceImpl`).
- [x] Integration & unit test suite passing cleanly with Maven (`mvn clean test`).
- [x] Docker Compose stack updated for containerized API, Scheduler, and Worker services.

## Key Decisions

- Single Spring Boot application codebase with profile/configuration toggles (`OMNISCRIBE_SCHEDULER_ENABLED`, `OMNISCRIBE_WORKER_ENABLED`).
- Flyway for database schema management under `src/main/resources/db/migration`.
- Strong static typing with Java records for DTOs and Kafka message schemas.
- Idiomatic Java camelCase field serialization for all DTOs and API responses.

## Next Actions & Roadmap

1. Implement Deficit Round Robin (DRR) Redis fair-share queuing in `SchedulerServiceImpl`.
2. Add WebSocket streaming endpoint for real-time segment updates.
3. Integrate Whisper model inference service boundary for worker transcription execution.