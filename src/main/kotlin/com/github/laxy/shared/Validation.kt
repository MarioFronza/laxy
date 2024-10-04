package com.github.laxy.shared

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

private const val MAX_EMAIL_LENGTH = 350
private const val MIN_USERNAME_LENGTH = 1
private const val MAX_USERNAME_LENGTH = 25
private val emailPattern = ".+@.+\\..+".toRegex()

private fun String.validateEmail(): InteractionResult<InvalidEmail, String> {
    val trimmed = trim()
    return listOf(
        trimmed.notBlank(),
        trimmed.maxSize(MAX_EMAIL_LENGTH),
        trimmed.looksLikeEmail()
    ).accumulateErrors(trimmed)
}

private fun String.notBlank(): InteractionResult<String, String> =
    if (isNotBlank()) Success("valid") else Failure("cannot be blank")

private fun String.minSize(size: Int): InteractionResult<String, String> =
    if (length >= size) Success("valid") else Failure("is too short (minimum is $size characters)")

private fun String.maxSize(size: Int): InteractionResult<String, String> =
    if (length <= size) Success("valid") else Failure("is too long (maximum is $size characters)")

private fun String.looksLikeEmail(): InteractionResult<String, String> =
    if (emailPattern.matches(this)) Success("valid") else Failure("'$this' is invalid email ")

private fun List<InteractionResult<String, String>>.accumulateErrors(
    value: String,
): InteractionResult<InvalidEmail, String> {
    val errors = this.filterIsInstance<Failure<String, String>>().map { it.error }
    return if (errors.isEmpty()) {
        Success(value)
    } else {
        Failure(InvalidEmail(errors))
    }
}