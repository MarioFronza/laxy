package com.github.laxy.domain.validation

import com.github.laxy.domain.validation.TextError.Companion.textError
import com.github.laxy.shared.ApplicationError
import com.github.laxy.shared.Failure
import com.github.laxy.shared.InteractionResult
import com.github.laxy.shared.Success

data class TextError(val message: String) : ApplicationError {
    companion object {
        fun textError(message: String) = TextError(message)
    }
}

fun String?.notNull(): InteractionResult<TextError, String> =
    if (this != null) Success(this) else Failure(textError("cannot be null"))

fun String.notBlank(): InteractionResult<TextError, String> =
    if (isNotBlank()) Success(this) else Failure(textError("cannot be blank"))

fun String.minSize(size: Int): InteractionResult<TextError, String> =
    if (length >= size) Success(this)
    else Failure(textError("is too short (minimum is $size characters)"))

fun String.maxSize(size: Int): InteractionResult<TextError, String> =
    if (length <= size) Success(this)
    else Failure(textError("is too long (maximum is $size characters)"))

fun <E : ApplicationError, ID, OD> List<InteractionResult<E, ID>>.accumulateErrors(
    value: OD,
    onError: (List<ApplicationError>) -> E
): InteractionResult<E, OD> {
    val errors = this.filterIsInstance<Failure<E, ID>>().map { it.error }
    return if (errors.isEmpty()) {
        Success(value)
    } else {
        Failure(onError(errors))
    }
}

fun InteractionResult<ApplicationError, String>.bindInteraction(
    value: String,
    onError: (ApplicationError) -> ApplicationError
): InteractionResult<ApplicationError, String> {
    return when (this) {
        is Failure<ApplicationError, String> -> Failure(onError(this.error))
        is Success<ApplicationError, String> -> Success(value)
    }
}

