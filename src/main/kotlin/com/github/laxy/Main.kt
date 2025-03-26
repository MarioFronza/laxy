package com.github.laxy

import arrow.continuations.SuspendApp
import arrow.continuations.ktor.server
import arrow.fx.coroutines.resourceScope
import com.github.laxy.env.Dependencies
import com.github.laxy.env.Env
import com.github.laxy.env.configure
import com.github.laxy.env.dependencies
import com.github.laxy.route.health
import com.github.laxy.route.routes
import com.github.laxy.web.configureTemplating
import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.Metrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.awaitCancellation

fun main(): Unit = SuspendApp {
    val env = Env()
    resourceScope {
        val dependencies = dependencies(env)
        server(Netty, host = env.http.host, port = env.http.port) { app(dependencies) }
        awaitCancellation()
    }
}

fun Application.app(module: Dependencies) {
    configure(module.jwtService)
    routes(module)
    health(module.healthCheck)
    configureTemplating(module.userService, module.quizService, module.subjectService)

    val prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    Metrics.globalRegistry.add(prometheusRegistry)

    routing {
        get("/metrics") {
            call.respondText(prometheusRegistry.scrape(), ContentType.Text.Plain)
        }
    }
}
