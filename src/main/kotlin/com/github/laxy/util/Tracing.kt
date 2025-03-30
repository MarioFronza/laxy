package com.github.laxy.util

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.extension.kotlin.asContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.withContext

val tracer: Tracer = GlobalOpenTelemetry.getTracer("coroutine")

suspend fun <T> withSpan(
    spanName: String,
    parameters: (SpanBuilder.() -> Unit) = {},
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    block: suspend (span: Span) -> T
): T = tracer.startSpan(spanName, parameters, coroutineContext, block)

suspend fun <T> Tracer.startSpan(
    spanName: String,
    parameters: (SpanBuilder.() -> Unit) = {},
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    block: suspend (span: Span) -> T
): T {
    val span: Span =
        this.spanBuilder(spanName).run {
            parameters()
            startSpan()
        }
    return withContext(coroutineContext + span.asContextElement()) {
        try {
            block(span)
        } catch (throwable: Throwable) {
            span.setStatus(StatusCode.ERROR)
            span.recordException(throwable)
            throw throwable
        } finally {
            span.end()
        }
    }
}
