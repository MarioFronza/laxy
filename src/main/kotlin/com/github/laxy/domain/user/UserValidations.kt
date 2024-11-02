package com.github.laxy.domain.user

import com.github.laxy.domain.validation.IncorrectInput
import com.github.laxy.domain.validation.InvalidEmail
import com.github.laxy.domain.validation.InvalidUsername
import com.github.laxy.domain.validation.TextError
import com.github.laxy.domain.validation.TextError.Companion.textError
import com.github.laxy.domain.validation.accumulateErrors
import com.github.laxy.domain.validation.maxSize
import com.github.laxy.domain.validation.minSize
import com.github.laxy.domain.validation.notBlank
import com.github.laxy.shared.ApplicationError
import com.github.laxy.shared.Failure
import com.github.laxy.shared.InteractionResult
import com.github.laxy.shared.Success

fun RegisterUser.validate(): InteractionResult<ApplicationError, RegisterUser> {
    return listOf(username.validUsername(), email.validateEmail()).accumulateErrors(this) { errors ->
        IncorrectInput(errors)
    }
}

fun UpdateUser.validate(): InteractionResult<ApplicationError, UpdateUser> {
    return listOfNotNull(username?.validUsername(), email?.validateEmail()).accumulateErrors(
        this
    ) { errors ->
        IncorrectInput(errors)
    }
}

private fun String.validateEmail(): InteractionResult<ApplicationError, String> {
    val trimmed = trim()
    return listOf(trimmed.notBlank(), trimmed.maxSize(MAX_EMAIL_LENGTH), trimmed.looksLikeEmail())
        .accumulateErrors(trimmed) { errors -> InvalidEmail(errors) }
}

private fun String.validUsername(): InteractionResult<ApplicationError, String> {
    val trimmed = trim()
    return listOf(
        trimmed.notBlank(),
        trimmed.minSize(MIN_USERNAME_LENGTH),
        trimmed.maxSize(MAX_USERNAME_LENGTH)
    ).accumulateErrors(trimmed) { errors -> InvalidUsername(errors) }
}

private fun String.looksLikeEmail(): InteractionResult<TextError, String> =
    if (emailPattern.matches(this)) Success("valid") else Failure(textError("'$this' is invalid email "))

private const val MAX_EMAIL_LENGTH = 350
private const val MIN_USERNAME_LENGTH = 1
private const val MAX_USERNAME_LENGTH = 25
private val emailPattern = ".+@.+\\..+".toRegex()
