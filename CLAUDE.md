# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./gradlew build

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests com.github.laxy.service.UserServiceSpec

# Lint (static analysis)
./gradlew detekt

# Format code
./gradlew spotlessApply

# Build fat JAR
./gradlew fatShadowJar

# Run locally (requires PostgreSQL)
./gradlew run

# Start local database only
docker compose -f sandbox/docker-compose.yml up -d

# Start full stack (app + database)
docker compose -f sandbox/docker-compose.yml --profile app up -d

# Start full stack + observability
docker compose -f sandbox/docker-compose.yml --profile app --profile observability up -d
```

## Architecture

**Laxy** is an AI-powered English quiz generation backend. Users authenticate, select a language/subject, and receive GPT-generated quizzes.

**Key technology choices:**
- **Ktor** — lightweight async HTTP framework (coroutine-based)
- **Arrow** — functional error handling via `Either<DomainError, T>` throughout the service layer
- **SQLDelight** — type-safe SQL with `.sq` schema files in `src/main/sqldelight/`
- **SuspendApp** — structured concurrency for app lifecycle with resource scopes
- **Kotest + TestContainers** — tests spin up a real PostgreSQL container
- **OpenTelemetry** — distributed tracing configured in `env/`

**Request flow:**
```
route/ (Ktor handlers) → service/ (business logic, returns Either) → persistence/ (SQLDelight queries)
```

**Error handling pattern:** Services return `Either<DomainError, T>`. Routes fold the Either to produce HTTP responses. Domain errors are defined in `DomainError.kt`.

**Dependency wiring:** Done in `env/` via Kotlin context receivers (`-Xcontext-receivers` compiler flag is enabled). The `Main.kt` entry point bootstraps all dependencies using `ResourceScope`.

**Database schema:** Defined in `.sq` files. Tables: Users, Quizzes, Questions, QuestionOptions, Subjects, Languages, UserProgress, UserThemes. Seed data (languages, subjects) is in `config/sql/init.sql`.

**Auth:** JWT tokens via `kjwt`. Auth logic lives in `auth/`.

**Frontend:** Server-side HTML via Thymeleaf configured in `web/`.