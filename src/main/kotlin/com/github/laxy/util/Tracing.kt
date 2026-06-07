package com.github.laxy.util

import arrow.core.Either
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode

fun <E, A> Either<E, A>.onLeftRecordSpan(): Either<E, A> = onLeft {
    Span.current().setStatus(StatusCode.ERROR)
}
