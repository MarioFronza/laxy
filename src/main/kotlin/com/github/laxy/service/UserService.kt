package com.github.laxy.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.github.laxy.DomainError
import com.github.laxy.EmptyUpdate
import com.github.laxy.auth.JwtToken
import com.github.laxy.persistence.UserId
import com.github.laxy.persistence.UserPersistence
import com.github.laxy.util.withSpan
import com.github.laxy.validation.validate

data class RegisterUser(val username: String, val email: String, val password: String)

data class Update(
    val userId: UserId,
    val username: String?,
    val email: String?,
    val password: String?
)

data class Login(val email: String, val password: String)

data class UserInfo(val username: String, val email: String)

data class CreateTheme(val userId: UserId, val description: String)

data class UserThemeInfo(val description: String)

interface UserService {
    suspend fun register(input: RegisterUser): Either<DomainError, JwtToken>

    suspend fun login(input: Login): Either<DomainError, Pair<JwtToken, UserInfo>>

    suspend fun update(input: Update): Either<DomainError, UserInfo>

    suspend fun getUser(userId: UserId): Either<DomainError, UserInfo>

    suspend fun getUser(username: String): Either<DomainError, UserInfo>

    suspend fun createTheme(input: CreateTheme): Either<DomainError, UserThemeInfo>
}

fun userService(persistence: UserPersistence, jwtService: JwtService) =
    object : UserService {
        val spanPrefix = "UserService"

        override suspend fun register(input: RegisterUser): Either<DomainError, JwtToken> =
            withSpan("$spanPrefix.register") { span ->
                either {
                    val (username, email, password) = input.validate().bind()
                    val userId = persistence.insert(username, email, password).bind()
                    span.setAttribute("user.id", userId.serial)
                    jwtService.generateJwtToken(userId).bind()
                }
            }

        override suspend fun login(input: Login): Either<DomainError, Pair<JwtToken, UserInfo>> =
            withSpan("$spanPrefix.login") { span ->
                either {
                    val (email, password) = input.validate().bind()
                    val (userId, info) = persistence.verifyPassword(email, password).bind()
                    span.setAttribute("user.id", userId.serial)
                    val token = jwtService.generateJwtToken(userId).bind()
                    Pair(token, info)
                }
            }

        override suspend fun update(input: Update): Either<DomainError, UserInfo> =
            withSpan("$spanPrefix.update") { span ->
                either {
                    val (userId, username, email, password) = input.validate().bind()
                    span.setAttribute("user.id", userId.serial)
                    ensure(email != null || username != null) {
                        EmptyUpdate("Cannot update user with $userId with only null values")
                    }
                    persistence.update(userId, username, email, password).bind()
                }
            }

        override suspend fun getUser(userId: UserId): Either<DomainError, UserInfo> =
            withSpan("$spanPrefix.getUser") { span ->
                span.setAttribute("user.id", userId.serial)
                persistence.select(userId)
            }

        override suspend fun getUser(username: String): Either<DomainError, UserInfo> =
            withSpan("$spanPrefix.getUser") { span ->
                span.setAttribute("user.username", username)
                persistence.select(username)
            }

        override suspend fun createTheme(input: CreateTheme): Either<DomainError, UserThemeInfo> =
            withSpan("$spanPrefix.getUser") { span ->
                either {
                    val (userId, description) = input.validate().bind()
                    span.setAttribute("user.id", userId.serial)
                    persistence.setCurrent(userId, isCurrent = false)
                    val theme = persistence.insertTheme(userId, description)
                    theme.bind()
                }
            }
    }
