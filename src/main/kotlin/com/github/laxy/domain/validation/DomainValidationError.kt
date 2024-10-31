package com.github.laxy.domain.validation

import com.github.laxy.shared.ApplicationError

sealed interface DomainError : ApplicationError

sealed interface ValidationError : DomainError

data class IncorrectInput(val errors: List<ApplicationError>) : ValidationError {
    constructor(head: InvalidField) : this(listOf(head))
}

sealed interface UserError : DomainError

data class UserNotFound(val property: String) : UserError

data class EmailAlreadyExists(val email: String) : UserError

data class UsernameAlreadyExists(val username: String) : UserError
