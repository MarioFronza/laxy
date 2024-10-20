package com.github.laxy.usecase.validation

import com.github.laxy.shared.Failure
import com.github.laxy.shared.InteractionResult
import com.github.laxy.shared.Success

sealed interface InvalidField {
    val errors: List<String>
    val field: String
}

data class InvalidEmail(override val errors: List<String>) : InvalidField {
    override val field = "email"
}

data class InvalidUsername(override val errors: List<String>) : InvalidField {
    override val field = "username"
}

fun String.notBlank(): InteractionResult<String, String> =
    if (isNotBlank()) Success(this) else Failure("cannot be blank")

fun String.minSize(size: Int): InteractionResult<String, String> =
    if (length >= size) Success(this) else Failure("is too short (minimum is $size characters)")

fun String.maxSize(size: Int): InteractionResult<String, String> =
    if (length <= size) Success(this) else Failure("is too long (maximum is $size characters)")

fun <IE, ID, OE, OD> List<InteractionResult<IE, ID>>.accumulateErrors(
    value: OD,
    onError: (List<IE>) -> OE
): InteractionResult<OE, OD> {
    val errors = this.filterIsInstance<Failure<IE, ID>>().map { it.error }
    return if (errors.isEmpty()) {
        Success(value)
    } else {
        Failure(onError(errors))
    }
}
