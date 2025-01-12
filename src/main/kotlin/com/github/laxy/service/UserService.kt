package com.github.laxy.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.github.laxy.DomainError
import com.github.laxy.EmptyUpdate
import com.github.laxy.auth.JwtToken
import com.github.laxy.persistence.UserId
import com.github.laxy.persistence.UserPersistence
import com.github.laxy.validate

data class RegisterUser(val username: String, val email: String, val password: String)

data class Update(
    val userId: UserId,
    val username: String?,
    val email: String?,
    val password: String?
)

data class Login(val email: String, val password: String)

data class UserInfo(val username: String, val email: String)

interface UserService {
    suspend fun register(input: RegisterUser): Either<DomainError, JwtToken>

    suspend fun login(input: Login): Either<DomainError, Pair<JwtToken, UserInfo>>

    suspend fun update(input: Update): Either<DomainError, UserInfo>

    suspend fun getUser(userId: UserId): Either<DomainError, UserInfo>

    suspend fun getUser(username: String): Either<DomainError, UserInfo>
}

fun userService(persistence: UserPersistence, jwtService: JwtService) =
    object : UserService {
        override suspend fun register(input: RegisterUser): Either<DomainError, JwtToken> = either {
            val (username, email, password) = input.validate().bind()
            val userId = persistence.insert(username, email, password).bind()
            return jwtService.generateJwtToken(userId)
        }

        override suspend fun login(input: Login): Either<DomainError, Pair<JwtToken, UserInfo>> =
            either {
                val (email, password) = input.validate().bind()
                val (userId, info) = persistence.verifyPassword(email, password).bind()
                val token = jwtService.generateJwtToken(userId).bind()
                Pair(token, info)
            }

        override suspend fun update(input: Update): Either<DomainError, UserInfo> = either {
            val (userId, username, email, password) = input.validate().bind()
            ensure(email != null || username != null) {
                EmptyUpdate("Cannot update user with $userId with only null values")
            }
            persistence.update(userId, username, email, password).bind()
        }

        override suspend fun getUser(userId: UserId): Either<DomainError, UserInfo> {
            return persistence.select(userId)
        }

        override suspend fun getUser(username: String): Either<DomainError, UserInfo> {
            return persistence.select(username)
        }
    }
