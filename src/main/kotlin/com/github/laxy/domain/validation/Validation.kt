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

data class InvalidPassword(override val errors: List<ApplicationError>) : InvalidField {
    override val field: String = "password"
}

fun String.validPassword(): InteractionResult<ApplicationError, String> =
    listOf(
        notBlank(),
        minSize(MIN_PASSWORD_LENGTH),
        maxSize(MAX_PASSWORD_LENGTH)
    ).accumulateErrors(this) { errors ->
        InvalidPassword(errors)
    }

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

private const val MIN_PASSWORD_LENGTH = 8
private const val MAX_PASSWORD_LENGTH = 100