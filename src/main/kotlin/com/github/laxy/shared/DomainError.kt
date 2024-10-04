package com.github.laxy.shared

sealed interface DomainError

sealed interface ValidationError : DomainError

sealed interface UserError : DomainError

data class UserNotFound(val property: String) : UserError

data class EmailAlreadyExists(val email: String) : UserError

data class UsernameAlreadyExists(val username: String) : UserError
