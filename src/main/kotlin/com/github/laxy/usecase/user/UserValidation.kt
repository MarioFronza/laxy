package com.github.laxy.usecase.user

import com.github.laxy.service.RegisterUser
import com.github.laxy.service.UpdateUser
import com.github.laxy.shared.Failure
import com.github.laxy.shared.InteractionResult
import com.github.laxy.shared.Success
import com.github.laxy.usecase.validation.IncorrectInput
import com.github.laxy.usecase.validation.InvalidEmail
import com.github.laxy.usecase.validation.InvalidField
import com.github.laxy.usecase.validation.InvalidUsername
import com.github.laxy.usecase.validation.accumulateErrors
import com.github.laxy.usecase.validation.maxSize
import com.github.laxy.usecase.validation.minSize
import com.github.laxy.usecase.validation.notBlank

fun RegisterUser.validate(): InteractionResult<IncorrectInput, RegisterUser> {
    return listOf(username.validUsername(), email.validateEmail()).accumulateErrors(this) { errors
        ->
        IncorrectInput(errors)
    }
}

fun UpdateUser.validate(): InteractionResult<IncorrectInput, UpdateUser> {
    return listOfNotNull(username?.validUsername(), email?.validateEmail()).accumulateErrors(
        this
    ) { errors ->
        IncorrectInput(errors)
    }
}

private fun String.validateEmail(): InteractionResult<InvalidField, String> {
    val trimmed = trim()
    return listOf(trimmed.notBlank(), trimmed.maxSize(MAX_EMAIL_LENGTH), trimmed.looksLikeEmail())
        .accumulateErrors(trimmed) { errors -> InvalidEmail(errors) }
}

private fun String.validUsername(): InteractionResult<InvalidField, String> {
    val trimmed = trim()
    return listOf(
            trimmed.notBlank(),
            trimmed.minSize(MIN_USERNAME_LENGTH),
            trimmed.maxSize(MAX_USERNAME_LENGTH)
        )
        .accumulateErrors(trimmed) { errors -> InvalidUsername(errors) }
}

private fun String.looksLikeEmail(): InteractionResult<String, String> =
    if (emailPattern.matches(this)) Success("valid") else Failure("'$this' is invalid email ")

private const val MAX_EMAIL_LENGTH = 350
private const val MIN_USERNAME_LENGTH = 1
private const val MAX_USERNAME_LENGTH = 25
private val emailPattern = ".+@.+\\..+".toRegex()
