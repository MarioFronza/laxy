# Laxy

AI‑assisted quiz generator built with Kotlin + Ktor. Users pick a language/subject, quizzes are generated via OpenAI and stored using SQLDelight. Includes a simple HTML UI (Thymeleaf) and observability via OpenTelemetry.

## Features
- Auth (JWT), subjects/languages, quiz creation and attempts
- Ktor server with typed routes and templates
- SQLDelight schema and queries, HikariCP, Postgres
- Test suite with Kotest and Testcontainers

## Tech Stack
- Kotlin 2.x, Ktor 2.x, Arrow, Kotlinx Serialization
- SQLDelight, HikariCP, PostgreSQL
- OpenAI client, OpenTelemetry, Logback
- Gradle (Kotlin DSL), Detekt, Spotless, Kover

## Quick Start
Prereqs: JDK 21, Docker (for tests), Git.

Build and run locally:
```bash
./gradlew build            # compile, lint (detekt), test
./gradlew run              # start server (http://localhost:8080)
```

Config via environment variables (defaults in `src/main/kotlin/com/github/laxy/env/Env.kt`):
`POSTGRES_URL`, `POSTGRES_USERNAME`, `POSTGRES_PASSWORD`, `OPENAI_TOKEN`, `JWT_*`, `OTEL_*`.

## Testing & Quality
```bash
./gradlew test             # runs Kotest (requires Docker for Testcontainers)
./gradlew koverHtmlReport  # open build/reports/kover/html/index.html
./gradlew spotlessApply    # format
./gradlew detekt           # lint
```

## Docker
Build and run the container image locally:
```bash
docker build -t laxy-app:local .
docker run --rm -p 8080:8080 \
  -e POSTGRES_URL="jdbc:postgresql://host.docker.internal:5432/laxy-database" \
  -e POSTGRES_USERNAME=postgres -e POSTGRES_PASSWORD=postgres \
  laxy-app:local
```

## Project Structure
- `src/main/kotlin/com/github/laxy/` – application code (routes, services, persistence, env)
- `src/main/resources/` – templates (Thymeleaf), static assets, logging
- `src/main/sqldelight/` – SQLDelight schema and queries
- `src/test/kotlin/` – tests (`*Spec.kt`)

## Contributing
See AGENTS.md for coding style, commands, testing, and PR guidelines. PR templates are provided under `.github/PULL_REQUEST_TEMPLATE/`.
