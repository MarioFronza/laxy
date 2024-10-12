package com.github.laxy.shared

import com.github.laxy.service.RegisterUser
import com.github.laxy.service.UpdateUser

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

fun RegisterUser.validate(): InteractionResult<IncorrectInput, RegisterUser> {
    return listOf(
        username.validUsername(),
        email.validateEmail()
    ).accumulateErrors(this) { errors -> IncorrectInput(errors) }
}

fun UpdateUser.validate(): InteractionResult<IncorrectInput, UpdateUser> {
    return listOfNotNull(
        username?.validUsername(),
        email?.validateEmail()
    ).accumulateErrors(this) { errors -> IncorrectInput(errors) }
}

private const val MAX_EMAIL_LENGTH = 350
private const val MIN_USERNAME_LENGTH = 1
private const val MAX_USERNAME_LENGTH = 25
private val emailPattern = ".+@.+\\..+".toRegex()

private fun String.validateEmail(): InteractionResult<InvalidField, String> {
    val trimmed = trim()
    return listOf(
        trimmed.notBlank(),
        trimmed.maxSize(MAX_EMAIL_LENGTH),
        trimmed.looksLikeEmail()
    ).accumulateErrors(trimmed) { errors -> InvalidEmail(errors) }
}

private fun String.validUsername(): InteractionResult<InvalidField, String> {
    val trimmed = trim()
    return listOf(
        trimmed.notBlank(),
        trimmed.minSize(MIN_USERNAME_LENGTH),
        trimmed.maxSize(MAX_USERNAME_LENGTH)
    ).accumulateErrors(trimmed) { errors -> InvalidUsername(errors) }
}

private fun String.notBlank(): InteractionResult<String, String> =
    if (isNotBlank()) Success(this) else Failure("cannot be blank")

private fun String.minSize(size: Int): InteractionResult<String, String> =
    if (length >= size) Success(this) else Failure("is too short (minimum is $size characters)")

private fun String.maxSize(size: Int): InteractionResult<String, String> =
    if (length <= size) Success(this) else Failure("is too long (maximum is $size characters)")

private fun String.looksLikeEmail(): InteractionResult<String, String> =
    if (emailPattern.matches(this)) Success("valid") else Failure("'$this' is invalid email ")


//TODO this could be generic
private fun List<InteractionResult<String, String>>.accumulateErrors(
    value: String,
    onError: (List<String>) -> InvalidField
): InteractionResult<InvalidField, String> {
    val errors = this.filterIsInstance<Failure<String, String>>().map { it.error }
    return if (errors.isEmpty()) {
        Success(value)
    } else {
        Failure(onError(errors))
    }
}

private fun List<InteractionResult<InvalidField, String>>.accumulateErrors(
    value: RegisterUser,
    onError: (List<InvalidField>) -> IncorrectInput
): InteractionResult<IncorrectInput, RegisterUser> {
    val errors = this.filterIsInstance<Failure<InvalidField, String>>().map { it.error }
    return if (errors.isEmpty()) {
        Success(value)
    } else {
        Failure(onError(errors))
    }
}

private fun List<InteractionResult<InvalidField, String>>.accumulateErrors(
    value: UpdateUser,
    onError: (List<InvalidField>) -> IncorrectInput
): InteractionResult<IncorrectInput, UpdateUser> {
    val errors = this.filterIsInstance<Failure<InvalidField, String>>().map { it.error }
    return if (errors.isEmpty()) {
        Success(value)
    } else {
        Failure(onError(errors))
    }
}