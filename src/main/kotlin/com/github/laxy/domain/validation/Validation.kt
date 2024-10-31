package com.github.laxy.domain.validation

import com.github.laxy.domain.validation.TextError.Companion.textError
import com.github.laxy.shared.ApplicationError
import com.github.laxy.shared.Failure
import com.github.laxy.shared.InteractionResult
import com.github.laxy.shared.Success

sealed interface InvalidField : ApplicationError {
    val errors: List<ApplicationError>
    val field: String
}

data class TextError(val message: String) : ApplicationError {
    companion object {
        fun textError(message: String) = TextError(message)
    }
}

data class InvalidEmail(override val errors: List<ApplicationError>) : InvalidField {
    override val field = "email"
}

data class InvalidUsername(override val errors: List<ApplicationError>) : InvalidField {
    override val field = "username"
}

fun String.notBlank(): InteractionResult<ApplicationError, String> =
    if (isNotBlank()) Success(this) else Failure(textError("cannot be blank"))

fun String.minSize(size: Int): InteractionResult<ApplicationError, String> =
    if (length >= size) Success(this) else Failure(textError("is too short (minimum is $size characters)"))

fun String.maxSize(size: Int): InteractionResult<ApplicationError, String> =
    if (length <= size) Success(this) else Failure(textError("is too long (maximum is $size characters)"))

fun <ID, OD> List<InteractionResult<ApplicationError, ID>>.accumulateErrors(
    value: OD,
    onError: (List<ApplicationError>) -> ApplicationError
): InteractionResult<ApplicationError, OD> {
    val errors = this.filterIsInstance<Failure<ApplicationError, ID>>().map { it.error }
    return if (errors.isEmpty()) {
        Success(value)
    } else {
        Failure(onError(errors))
    }
}
