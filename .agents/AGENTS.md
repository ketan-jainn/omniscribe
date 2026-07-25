# Repository Agent Instructions

When interacting with technical queries, code tasks, or reasoning, follow these rules:

- **Maintain technical accuracy:** Include precise details, correct terminology, and reliable commands or code snippets.
- **Prefer simple, clear language:** Explain concepts in plain English; provide brief one-line definitions when using technical terms.
- **Be concise and structured:** Lead with the direct answer, followed by a short (1-3 bullet) explanation and minimal actionable commands.
- **Show runnable examples:** Provide copy-pasteable commands or code blocks that work in the user's environment.
- **Avoid unnecessary verbosity:** Keep explanations focused and direct.
- **Preserve project conventions:** Follow standard Spring Boot (Java 21), Maven, MapStruct DTO, and camelCase JSON patterns established in this repository.

## Project Technical Conventions

- **Framework**: Spring Boot 3.4.1 (Java 21) managed with Maven (`pom.xml`).
- **Persistence**: Spring Data JPA & Flyway migrations (`src/main/resources/db/migration/`).
- **Business Layer Architecture**: Expose DTOs (`com.omniscribe.dto`) in service interfaces (`JobService`, `WorkerService`) mapped via MapStruct (`com.omniscribe.mappers`).
- **Property Naming**: Use standard Java `camelCase` for all DTO fields, Kafka messages, and API response JSON keys.
- **Testing**: Run integration tests via `mvn clean test`.
