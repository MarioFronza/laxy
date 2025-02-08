package com.github.laxy

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import com.github.laxy.validation.InvalidField
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException

sealed interface DomainError

sealed interface ValidationError : DomainError

@OptIn(ExperimentalSerializationApi::class)
data class IncorrectJson(val exception: MissingFieldException) : ValidationError

data class EmptyUpdate(val description: String) : ValidationError

data class IncorrectInput(val errors: NonEmptyList<InvalidField>) : ValidationError {
    constructor(head: InvalidField) : this(nonEmptyListOf(head))
}

data class MissingParameter(val name: String) : ValidationError

sealed interface UserError : DomainError

sealed interface SubjectError : DomainError

data class UserNotFound(val property: String) : UserError

data class EmailAlreadyExists(val email: String) : UserError

data class UsernameAlreadyExists(val username: String) : UserError

data class UserThemeNotFound(val property: String) : UserError

data object PasswordNotMatched : UserError

data class SubjectNotFound(val property: String) : SubjectError

sealed interface JwtError : DomainError

data class JwtGeneration(val description: String) : JwtError

data class JwtInvalid(val description: String) : JwtError

sealed interface GptIntegrationError : DomainError

data class InvalidIntegrationResponse(val content: String?) : GptIntegrationError
