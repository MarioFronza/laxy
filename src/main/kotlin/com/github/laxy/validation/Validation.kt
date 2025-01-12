package com.github.laxy.validation

import arrow.core.Either
import arrow.core.Either.Companion.zipOrAccumulate
import arrow.core.EitherNel
import arrow.core.NonEmptyList
import arrow.core.leftNel
import arrow.core.nonEmptyListOf
import arrow.core.right
import com.github.laxy.IncorrectInput
import com.github.laxy.service.Login
import com.github.laxy.service.RegisterUser
import com.github.laxy.service.Update

sealed interface InvalidField {
    val errors: NonEmptyList<String>
    val field: String
}

data class InvalidEmail(override val errors: NonEmptyList<String>) : InvalidField {
    override val field = "email"
}

data class InvalidPassword(override val errors: NonEmptyList<String>) : InvalidField {
    override val field = "password"
}

data class InvalidUsername(override val errors: NonEmptyList<String>) : InvalidField {
    override val field = "username"
}

fun Login.validate(): Either<IncorrectInput, Login> =
    zipOrAccumulate(email.validEmail(), password.validPassword(), ::Login).mapLeft(::IncorrectInput)

fun RegisterUser.validate(): Either<IncorrectInput, RegisterUser> =
    zipOrAccumulate(
            username.validUsername(),
            email.validEmail(),
            password.validPassword(),
            ::RegisterUser
        )
        .mapLeft(::IncorrectInput)

fun Update.validate(): Either<IncorrectInput, Update> =
    zipOrAccumulate(
            username.mapOrAccumulate(String::validUsername),
            email.mapOrAccumulate(String::validEmail),
            password.mapOrAccumulate(String::validPassword)
        ) { username, email, password ->
            Update(userId, username, email, password)
        }
        .mapLeft(::IncorrectInput)

private fun String.validPassword(): EitherNel<InvalidPassword, String> =
    zipOrAccumulate(notBlank(), minSize(MIN_PASSWORD_LENGTH), maxSize(MAX_PASSWORD_LENGTH)) {
            a,
            _,
            _ ->
            a
        }
        .mapLeft(toInvalidField(::InvalidPassword))

private fun String.validEmail(): EitherNel<InvalidEmail, String> {
    val trimmed = trim()
    return zipOrAccumulate(
            trimmed.notBlank(),
            trimmed.maxSize(MAX_EMAIL_LENGTH),
            trimmed.looksLikeEmail()
        ) { a, _, _ ->
            a
        }
        .mapLeft(toInvalidField(::InvalidEmail))
}

private fun String.validUsername(): EitherNel<InvalidUsername, String> {
    val trimmed = trim()
    return zipOrAccumulate(
            trimmed.notBlank(),
            trimmed.minSize(MIN_USERNAME_LENGTH),
            trimmed.maxSize(MAX_USERNAME_LENGTH)
        ) { a, _, _ ->
            a
        }
        .mapLeft(toInvalidField(::InvalidUsername))
}

private fun <A, E, B> A?.mapOrAccumulate(f: (A) -> EitherNel<E, B>): EitherNel<E, B?> =
    this?.let(f) ?: null.right()

private fun <A : InvalidField> toInvalidField(
    transform: (NonEmptyList<String>) -> A
): (NonEmptyList<String>) -> NonEmptyList<A> = { nel -> nonEmptyListOf(transform(nel)) }

private fun String.looksLikeEmail(): EitherNel<String, String> =
    if (emailPattern.matches(this)) right() else "'$this' is invalid email".leftNel()

private const val MIN_PASSWORD_LENGTH = 8
private const val MAX_PASSWORD_LENGTH = 100
private const val MAX_EMAIL_LENGTH = 350
private const val MIN_USERNAME_LENGTH = 1
private const val MAX_USERNAME_LENGTH = 25
private val emailPattern = ".+@.+\\..+".toRegex()
