package com.github.laxy.domain.user

import com.github.laxy.domain.validation.accumulateErrors
import com.github.laxy.domain.validation.maxSize
import com.github.laxy.domain.validation.minSize
import com.github.laxy.domain.validation.notBlank
import com.github.laxy.shared.ApplicationError
import com.github.laxy.shared.Failure
import com.github.laxy.shared.InteractionResult
import com.github.laxy.shared.Success
import com.github.laxy.shared.IllegalStateError.Companion.illegalState


fun RegisterUser.validate(): InteractionResult<ApplicationError, RegisterUser> {
    return listOf(username.validUsername(), email.validateEmail()).accumulateErrors(this)
}

fun UpdateUser.validate(): InteractionResult<ApplicationError, UpdateUser> {
    return listOfNotNull(username?.validUsername(), email?.validateEmail()).accumulateErrors(this)
}

fun String.validateEmail(): InteractionResult<ApplicationError, String> {
    val trimmed = trim()
    return listOf(trimmed.notBlank(), trimmed.maxSize(MAX_EMAIL_LENGTH), trimmed.looksLikeEmail())
        .accumulateErrors(trimmed)
}

fun String.validUsername(): InteractionResult<ApplicationError, String> {
    val trimmed = trim()
    return listOf(
            trimmed.notBlank(),
            trimmed.minSize(MIN_USERNAME_LENGTH),
            trimmed.maxSize(MAX_USERNAME_LENGTH)
        )
        .accumulateErrors(trimmed)
}

fun String.validPassword(): InteractionResult<ApplicationError, String> =
    listOf(
        notBlank(),
        minSize(MIN_PASSWORD_LENGTH),
        maxSize(MAX_PASSWORD_LENGTH)
    ).accumulateErrors(this)

private fun String.looksLikeEmail(): InteractionResult<ApplicationError, String> =
    if (emailPattern.matches(this)) Success("valid")
    else Failure(illegalState("'$this' is invalid email "))

private const val MAX_EMAIL_LENGTH = 350
private const val MIN_USERNAME_LENGTH = 1
private const val MAX_USERNAME_LENGTH = 25
private const val MIN_PASSWORD_LENGTH = 8
private const val MAX_PASSWORD_LENGTH = 100
private val emailPattern = ".+@.+\\..+".toRegex()
