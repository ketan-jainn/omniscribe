# Future Architecture Roadmap: Maven Multi-Module Microservices

This document outlines the design, module topology, build configuration, and migration roadmap for evolving Omniscribe from a single-jar multi-profile architecture into a **true Maven Multi-Module Microservices Repository**.

---

## 1. Motivation & Architectural Vision

Currently, Omniscribe compiles into a single Spring Boot artifact that toggles features via environment variables (`OMNISCRIBE_SCHEDULER_ENABLED`, `OMNISCRIBE_WORKER_ENABLED`). 

Evolving into a **multi-module Maven layout** provides:
- **Strict Code & Boundary Isolation**: API code cannot leak into worker processing; shared domain entities live in a dedicated core library.
- **Independent Scaling & Container Footprints**: Each microservice (`api`, `scheduler`, `worker`) builds its own minimal Docker container without unused dependencies or endpoints.
- **Independent Deployment Lifecycles**: Deploy API updates without re-building worker listeners or database migration tools.

---

## 2. Target Project Topology

```
omniscribe/
├── pom.xml                        # Parent POM (manages spring-boot-starter-parent, versions, modules)
├── omniscribe-core/               # Shared Domain & Infrastructure Library
│   ├── pom.xml
│   └── src/main/java/com/omniscribe/
│       ├── dto/                   # Business Layer DTOs (JobDto, UserDto, ChunkDto, SegmentDto)
│       ├── mappers/               # MapStruct Interfaces (JobMapper, UserMapper, ChunkMapper, SegmentMapper)
│       ├── models/                # JPA Entities & Kafka Message Contracts
│       ├── repositories/          # Spring Data JPA Repositories
│       ├── messaging/             # KafkaPublisher Component
│       └── config/                # Shared Kafka & Database Configuration
├── omniscribe-api/                # HTTP REST API Microservice
│   ├── pom.xml
│   ├── Dockerfile                 # Dedicated API Container Definition
│   └── src/
│       ├── main/java/com/omniscribe/api/
│       │   ├── ApiApplication.java # Spring Boot Entrypoint
│       │   └── controllers/       # JobController, HealthController, RootController, ExceptionHandlers
│       └── main/resources/
│           └── application-api.yml
├── omniscribe-scheduler/          # Ingress Scheduler Microservice
│   ├── pom.xml
│   ├── Dockerfile                 # Dedicated Scheduler Container Definition
│   └── src/
│       ├── main/java/com/omniscribe/scheduler/
│       │   ├── SchedulerApplication.java # Spring Boot Entrypoint
│       │   └── listener/          # Ingress Consumer & DRR Fair-Share Logic
│       └── main/resources/
│           └── application-scheduler.yml
└── omniscribe-worker/             # Transcription & Segment Worker Microservice
    ├── pom.xml
    ├── Dockerfile                 # Dedicated Worker Container Definition
    └── src/
        ├── main/java/com/omniscribe/worker/
        │   ├── WorkerApplication.java # Spring Boot Entrypoint
        │   └── listener/          # Jobs Consumer & Segment Persistence Service
        └── main/resources/
            └── application-worker.yml
```

---

## 3. Module Responsibilities & Dependencies

### A. `omniscribe-core` (Library Module)
- **Output**: Standard `.jar` library (non-executable).
- **Dependencies**: `spring-boot-starter-data-jpa`, `flyway-core`, `postgresql`, `mapstruct`, `spring-kafka`.
- **Responsibilities**: Defines database entities, Flyway migrations (`db/migration`), Spring Data JPA repositories, MapStruct DTO mappers, and common Kafka topic definitions.

### B. `omniscribe-api` (Executable Microservice)
- **Output**: Executable Spring Boot `.jar` + Docker Image.
- **Dependencies**: `omniscribe-core`, `spring-boot-starter-web`, `spring-boot-starter-validation`, `spring-boot-starter-actuator`.
- **Responsibilities**: Exposes REST endpoints (`/v1/jobs`, `/health/*`), handles HTTP idempotency, CORS, and request validations.

### C. `omniscribe-scheduler` (Executable Microservice)
- **Output**: Executable Spring Boot `.jar` + Docker Image.
- **Dependencies**: `omniscribe-core`, `spring-boot-starter-actuator`, `spring-boot-starter-data-redis`.
- **Responsibilities**: Listens to `transcription.ingress`, executes passthrough or Deficit Round Robin (DRR) fair-share queueing using Redis, and publishes work to `transcription.jobs`.

### D. `omniscribe-worker` (Executable Microservice)
- **Output**: Executable Spring Boot `.jar` + Docker Image.
- **Dependencies**: `omniscribe-core`, `spring-boot-starter-actuator`.
- **Responsibilities**: Consumes from `transcription.jobs`, executes chunk transcription, persists segment rows to PostgreSQL, and emits segment updates onto `transcription.segments`.

---

## 4. Parent `pom.xml` Structure Example

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" ...>
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.omniscribe</groupId>
    <artifactId>omniscribe-parent</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.1</version>
    </parent>

    <modules>
        <module>omniscribe-core</module>
        <module>omniscribe-api</module>
        <module>omniscribe-scheduler</module>
        <module>omniscribe-worker</module>
    </modules>

    <properties>
        <java.version>21</java.version>
        <org.mapstruct.version>1.6.3</org.mapstruct.version>
    </properties>
</project>
```

---

## 5. Docker Compose System Integration

With multi-module microservices, `docker-compose.yml` targets each submodule cleanly:

```yaml
services:
  omniscribe-api:
    build:
      context: ./omniscribe-api
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    depends_on:
      postgres: { condition: service_healthy }
      redpanda: { condition: service_healthy }

  omniscribe-scheduler:
    build:
      context: ./omniscribe-scheduler
      dockerfile: Dockerfile
    depends_on:
      postgres: { condition: service_healthy }
      redpanda: { condition: service_healthy }

  omniscribe-worker:
    build:
      context: ./omniscribe-worker
      dockerfile: Dockerfile
    depends_on:
      postgres: { condition: service_healthy }
      redpanda: { condition: service_healthy }
```

---

## 6. Migration Execution Steps

When ready to transition to this multi-module layout:

1. Convert root `pom.xml` to `<packaging>pom</packaging>` and declare `<modules>`.
2. Create `omniscribe-core/` directory and move `dto`, `mappers`, `models`, `repositories`, and `db/migration` into `omniscribe-core`.
3. Create `omniscribe-api/`, `omniscribe-scheduler/`, and `omniscribe-worker/` directories with dedicated `@SpringBootApplication` main classes.
4. Add `omniscribe-core` dependency in each microservice module's `pom.xml`.
5. Create lightweight Dockerfiles inside each subservice directory.
6. Update `docker-compose.yml` build contexts to reference module subdirectories.
