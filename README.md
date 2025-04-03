# Laxy

Laxy is a playful project that combines AI and backend development to generate educational quizzes. It's designed to
help users practice English through GPT-generated quizzes based on selected themes and subjects. While currently focused
on English, it's built to support other languages in the future.

## Project Goal

The primary goal is to explore how to integrate modern backend technologies with AI services in a clean and scalable
architecture. It serves as a learning platform for:

- Structuring production-grade Kotlin applications
- Integrating with OpenAI APIs
- Leveraging functional programming patterns
- Practicing observability and clean architecture

## Features

- User registration and authentication
- Language and subject selection
- Quiz creation using OpenAI completions
- GPT response parsing and persistence
- Dynamic HTML frontend with templating

## Tech Overview

- **Kotlin** + **Ktor** for the backend
- **SQLDelight** for typesafe database access
- **OpenAI GPT** for quiz generation
- **Arrow** for functional constructs
- **OpenTelemetry** for tracing and metrics
- **Kotest** for tests
- **Docker & Docker Compose** for containerization

## Running

To run locally:

```bash
./gradlew build
docker-compose up
