package com.github.laxy.env

import com.github.laxy.auth.optionalJwtAuth
import com.github.laxy.route.LoginUser
import com.github.laxy.route.UserWrapper
import com.github.laxy.service.JwtService
import com.github.laxy.util.tracer
import com.github.laxy.web.CurrentUserId
import com.github.laxy.web.UserSession
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.session
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.maxAgeDuration
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.resources.Resources
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Routing
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME
import java.time.Duration
import kotlin.time.Duration.Companion.days
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

val kotlinXSerializersModule = SerializersModule {
    contextual(UserWrapper::class) { UserWrapper.serializer(LoginUser.serializer()) }
    polymorphic(Any::class) { subclass(LoginUser::class, LoginUser.serializer()) }
}

fun Application.configure(jwtService: JwtService) {
    initOpenTelemetry()
    install(Routing) {
        intercept(ApplicationCallPipeline.Monitoring) {
            val path = call.request.path()
            if (path.startsWith("/static") || path == "/favicon.ico") {
                proceed()
            } else {
                val span =
                    tracer
                        .spanBuilder("[HTTP] - ${call.request.httpMethod.value} $path")
                        .setSpanKind(SpanKind.SERVER)
                        .startSpan()

                span.makeCurrent().use {
                    try {
                        proceed()
                    } catch (e: Throwable) {
                        span.recordException(e)
                        span.setStatus(StatusCode.ERROR)
                        throw e
                    } finally {
                        span.end()
                    }
                }
            }
        }
    }
    install(DefaultHeaders)
    install(Resources) { serializersModule = kotlinXSerializersModule }
    install(ContentNegotiation) {
        json(
            Json {
                serializersModule = kotlinXSerializersModule
                isLenient = true
                ignoreUnknownKeys = true
            }
        )
    }
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowNonSimpleContentTypes = true
        maxAgeDuration = 3.days
    }
    install(Sessions) {
        cookie<UserSession>("user_session") {
            cookie.path = "/"
            @Suppress("MagicNumber")
            cookie.maxAgeInSeconds = 7 * 24 * 60 * 60 // 1 week
            cookie.httpOnly = true // Prevent JavaScript access
        }
    }
    install(Authentication) {
        session<UserSession>("auth-session") {
            validate { session ->
                val jwtContext = optionalJwtAuth(jwtService, session.token)
                if (jwtContext != null) {
                    CurrentUserId(jwtContext.userId)
                } else {
                    null
                }
            }
            challenge { call.respondRedirect("/signin") }
        }
    }
}

fun initOpenTelemetry(): OpenTelemetry {
    val resource = Resource.getDefault().merge(Resource.create(Attributes.of(SERVICE_NAME, "laxy")))

    val metricExporter =
        OtlpGrpcMetricExporter.builder().setEndpoint("http://localhost:4317").build()
    val tracerExporter = OtlpGrpcSpanExporter.builder().setEndpoint("http://localhost:4317").build()
    val loggerExporter =
        OtlpGrpcLogRecordExporter.builder().setEndpoint("http://localhost:4317").build()

    val tracerProvider =
        SdkTracerProvider.builder()
            .setResource(resource)
            .addSpanProcessor(SimpleSpanProcessor.create(tracerExporter))
            .build()

    val metricProvider =
        SdkMeterProvider.builder()
            .setResource(resource)
            .registerMetricReader(
                PeriodicMetricReader.builder(metricExporter)
                    .setInterval(Duration.ofSeconds(10))
                    .build()
            )
            .build()

    val loggerProvider =
        SdkLoggerProvider.builder()
            .setResource(resource)
            .addLogRecordProcessor(BatchLogRecordProcessor.builder(loggerExporter).build())
            .build()

    val openTelemetry =
        OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .setMeterProvider(metricProvider)
            .setLoggerProvider(loggerProvider)
            .buildAndRegisterGlobal()

    OpenTelemetryAppender.install(openTelemetry)
    return openTelemetry
}
