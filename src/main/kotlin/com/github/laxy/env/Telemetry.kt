package com.github.laxy.env

import io.opentelemetry.api.common.Attributes
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

fun otel(env: Env.OpenTelemetry) {
    val resource =
        Resource.getDefault().merge(Resource.create(Attributes.of(SERVICE_NAME, env.serviceName)))

    val metricExporter = OtlpGrpcMetricExporter.builder().setEndpoint(env.endpoint).build()
    val tracerExporter = OtlpGrpcSpanExporter.builder().setEndpoint(env.endpoint).build()
    val loggerExporter = OtlpGrpcLogRecordExporter.builder().setEndpoint(env.endpoint).build()

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
                    .setInterval(Duration.ofSeconds(env.metricIntervalSeconds))
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
}
