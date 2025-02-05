package com.github.laxy.route

import arrow.core.raise.either
import com.github.laxy.service.GptAIService
import com.github.laxy.service.QuizService
import com.sksamuel.cohort.Cohort
import com.sksamuel.cohort.HealthCheckRegistry
import io.ktor.resources.Resource
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.Route
import io.ktor.server.resources.post

fun Application.health(healthCheck: HealthCheckRegistry) {
    install(Cohort) { healthcheck("/readiness", healthCheck) }
}
