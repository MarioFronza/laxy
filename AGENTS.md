# Repository Guidelines

## Project Structure & Module Organization
- Source: `src/main/kotlin/com/github/laxy/...` (Ktor server, services, routes, env, persistence, utils).
- Tests: `src/test/kotlin/com/github/laxy/...` using Kotest (`*Spec.kt`).
- Resources: `src/main/resources` (Thymeleaf templates in `templates/`, static assets in `static/`, logging in `logback.xml`).
- SQLDelight: `src/main/sqldelight/com/github/laxy/sqldelight/*.sq` for schema and queries.
- Build tooling: Gradle Kotlin DSL (`build.gradle.kts`, `libs.versions.toml`), CI in `.github/workflows/`.

## Build, Test, and Development Commands
- `./gradlew build`: Compile, run Detekt, and package artifacts.
- `./gradlew test`: Run Kotest on JUnit Platform. Requires Docker for Testcontainers.
- `./gradlew run`: Start the Ktor server (main class `com.github.laxy.MainKt`).
- `./gradlew fatShadowJar`: Build runnable jar `build/libs/laxy-app-fat.jar`.
- `./gradlew spotlessApply detekt`: Auto-format then lint.
- `./gradlew koverHtmlReport`: Generate coverage at `build/reports/kover/html/index.html`.

## Coding Style & Naming Conventions
- Language: Kotlin 2.x, 4-space indentation, official Kotlin style.
- Formatting: Spotless + ktfmt (Kotlinlang style). Run `./gradlew spotlessApply` before commits.
- Linting: Detekt runs in CI and on `build`.
- Packages: `com.github.laxy.*`. Tests end with `Spec` (e.g., `UserServiceSpec.kt`).
- SQLDelight: place `.sq` files under the `sqldelight/` package path.

## Testing Guidelines
- Frameworks: Kotest + Testcontainers + Ktor test utilities.
- Conventions: Mirror package structure; name files `*Spec.kt` and prefer descriptive `should ...` test names.
- Coverage: Keep regressions visible; check Kover HTML report locally.
- Running locally: Ensure Docker is running for containerized tests.

## Commit & Pull Request Guidelines
- Commits: Use concise messages; Conventional Commit prefixes encouraged (`feat:`, `fix:`, `test:`, `chore:`). Example: `feat: add quiz attempt tracking`.
- PRs: Include a clear description, linked issues, and screenshots for UI changes. Ensure CI passes and add notes on testing and configuration.

## Security & Configuration Tips
- Configure via environment variables: `POSTGRES_URL`, `POSTGRES_USERNAME`, `POSTGRES_PASSWORD`, `OPENAI_TOKEN`, `JWT_*`, `OTEL_*` (see `Env.kt`).
- Local defaults target `localhost`; do not commit secrets. For prod/dev containers, build with the provided `Dockerfile` or follow the CI workflow.
