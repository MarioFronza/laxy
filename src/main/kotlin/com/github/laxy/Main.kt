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
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import kotlinx.coroutines.awaitCancellation
import org.slf4j.LoggerFactory

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

    val logger = LoggerFactory.getLogger("Application")

    val tracerProvider = SdkTracerProvider.builder()
        .addSpanProcessor(
            SimpleSpanProcessor.create(
                OtlpGrpcSpanExporter.builder()
                    .setEndpoint("http://localhost:4317")
                    .build()
            )
        )
        .build()

    val openTelemetry = OpenTelemetrySdk.builder()
        .setTracerProvider(tracerProvider)
        .build()

    GlobalOpenTelemetry.set(openTelemetry)
    val tracer = openTelemetry.getTracer("laxy-app")

    routing {
        get("/metrics") {
            call.respondText(prometheusRegistry.scrape(), ContentType.Text.Plain)
        }

        get("/hello") {
            val span = tracer.spanBuilder("hello-handler").startSpan()
            span.setAttribute("custom.attribute", "Ktor is cool")

            logger.info("Handling /hello request")

            call.respondText("Hello, Observability 👀")
            span.end()
        }
    }
}
