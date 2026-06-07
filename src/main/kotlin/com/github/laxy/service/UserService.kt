package com.github.laxy.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.github.laxy.DomainError
import com.github.laxy.EmptyUpdate
import com.github.laxy.auth.JwtToken
import com.github.laxy.persistence.UserId
import com.github.laxy.persistence.UserPersistence
import com.github.laxy.util.logger
import com.github.laxy.util.onLeftRecordSpan
import com.github.laxy.util.resultAttributes
import com.github.laxy.util.userLoginCounter
import com.github.laxy.util.userRegisteredCounter
import com.github.laxy.validation.validate
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan

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
        val log = logger()

        @WithSpan("UserService.register")
        override suspend fun register(input: RegisterUser): Either<DomainError, JwtToken> =
            either {
                    val (username, email, password) = input.validate().bind()
                    val userId = persistence.insert(username, email, password).bind()
                    Span.current().setAttribute("user.id", userId.serial)
                    log.info("User registered: username={}", username)
                    jwtService.generateJwtToken(userId).bind()
                }
                .onLeftRecordSpan()
                .also { userRegisteredCounter.add(1, resultAttributes(it.isRight())) }
                .onLeft { log.warn("Registration failed for email={}: {}", input.email, it) }

        @WithSpan("UserService.login")
        override suspend fun login(input: Login): Either<DomainError, Pair<JwtToken, UserInfo>> =
            either {
                    val (email, password) = input.validate().bind()
                    val (userId, info) = persistence.verifyPassword(email, password).bind()
                    Span.current().setAttribute("user.id", userId.serial)
                    log.info("User logged in: email={}", email)
                    val token = jwtService.generateJwtToken(userId).bind()
                    Pair(token, info)
                }
                .onLeftRecordSpan()
                .also { userLoginCounter.add(1, resultAttributes(it.isRight())) }
                .onLeft { log.warn("Login failed for email={}: {}", input.email, it) }

        @WithSpan("UserService.update")
        override suspend fun update(input: Update): Either<DomainError, UserInfo> {
            Span.current().setAttribute("user.id", input.userId.serial)
            return either {
                    val (userId, username, email, password) = input.validate().bind()
                    ensure(email != null || username != null) {
                        EmptyUpdate("Cannot update user with $userId with only null values")
                    }
                    persistence.update(userId, username, email, password).bind()
                }
                .onLeftRecordSpan()
                .onLeft { log.warn("Update failed for userId={}: {}", input.userId.serial, it) }
        }

        @WithSpan("UserService.getUser")
        override suspend fun getUser(userId: UserId): Either<DomainError, UserInfo> {
            Span.current().setAttribute("user.id", userId.serial)
            return persistence.select(userId)
        }

        @WithSpan("UserService.getUser")
        override suspend fun getUser(username: String): Either<DomainError, UserInfo> {
            Span.current().setAttribute("user.username", username)
            return persistence.select(username)
        }

        @WithSpan("UserService.createTheme")
        override suspend fun createTheme(input: CreateTheme): Either<DomainError, UserThemeInfo> {
            Span.current().setAttribute("user.id", input.userId.serial)
            return either {
                    val (userId, description) = input.validate().bind()
                    persistence.setCurrent(userId, isCurrent = false)
                    persistence.insertTheme(userId, description).bind()
                }
                .onLeftRecordSpan()
                .onLeft { log.warn("CreateTheme failed for userId={}: {}", input.userId.serial, it) }
        }
    }
