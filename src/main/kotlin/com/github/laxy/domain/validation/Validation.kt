package com.github.laxy.domain.validation

import com.github.laxy.shared.ApplicationError
import com.github.laxy.shared.Failure
import com.github.laxy.shared.InteractionResult
import com.github.laxy.shared.IllegalStateError
import com.github.laxy.shared.IllegalStateError.Companion.illegalState
import com.github.laxy.shared.Success


fun String?.notNull(): InteractionResult<IllegalStateError, String> =
    if (this != null) Success(this) else Failure(illegalState("cannot be null"))

fun String.notBlank(): InteractionResult<IllegalStateError, String> =
    if (isNotBlank()) Success(this) else Failure(illegalState("cannot be blank"))

fun String.minSize(size: Int): InteractionResult<IllegalStateError, String> =
    if (length >= size) Success(this)
    else Failure(illegalState("is too short (minimum is $size characters)"))

fun String.maxSize(size: Int): InteractionResult<IllegalStateError, String> =
    if (length <= size) Success(this)
    else Failure(illegalState("is too long (maximum is $size characters)"))

fun <ID, OD> List<InteractionResult<ApplicationError, ID>>.accumulateErrors(
    value: OD
): InteractionResult<ApplicationError, OD> {
    val errors = this.filterIsInstance<Failure<ApplicationError, ID>>().map { it.error }
    return if (errors.isEmpty()) {
        Success(value)
    } else {
        Failure(IncorrectBehavior(errors))
    }
}

