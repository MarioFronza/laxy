package com.github.laxy.shared

sealed interface DomainError

sealed interface ValidationError : DomainError

data class IncorrectInput(val errors: List<InvalidField>) : ValidationError {
    constructor(head: InvalidField) : this(listOf(head))
}

sealed interface UserError : DomainError

data class UserNotFound(val property: String) : UserError

data class EmailAlreadyExists(val email: String) : UserError

data class UsernameAlreadyExists(val username: String) : UserError
