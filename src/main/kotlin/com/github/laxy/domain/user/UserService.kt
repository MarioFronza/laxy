package com.github.laxy.domain.user

import com.github.laxy.auth.JwtToken
import com.github.laxy.domain.auth.JwtService
import com.github.laxy.domain.auth.jwtService
import com.github.laxy.domain.auth.validate
import com.github.laxy.persistence.UserId
import com.github.laxy.persistence.UserPersistence
import com.github.laxy.shared.Failure
import com.github.laxy.shared.IllegalStateError
import com.github.laxy.shared.IllegalStateError.Companion.illegalState
import com.github.laxy.shared.InteractionResult
import com.github.laxy.shared.Success
import com.github.laxy.shared.interaction

data class RegisterUser(val username: String, val email: String, val password: String)

data class UpdateUser(
    val userId: UserId,
    val username: String?,
    val email: String?,
    val password: String?
)

data class Login(val email: String, val password: String)

data class UserInfo(val username: String, val email: String)

interface UserService {
    suspend fun register(input: RegisterUser): InteractionResult<UserId>

    suspend fun login(input: Login): InteractionResult<Pair<JwtToken, UserInfo>>

    suspend fun update(input: UpdateUser): InteractionResult<UserInfo>

    suspend fun getUser(userId: UserId): InteractionResult<UserInfo>

    suspend fun getUser(username: String): InteractionResult<UserInfo>
}

fun userService(persistence: UserPersistence, jwtService: JwtService) =
    object : UserService {
        override suspend fun register(input: RegisterUser): InteractionResult<UserId> =
            interaction {
                val (username, email, password) = input.validate().bind()
                val userId = persistence.insert(username, email, password).bind()
                return Success(userId)
            }

        override suspend fun login(input: Login): InteractionResult<Pair<JwtToken, UserInfo>> = interaction {
            val (email, password) = input.validate().bind()
            val (userId, info) = persistence.verifyPassword(email, password).bind()
            val token = jwtService.generateJwtToken(userId).bind()
            return Success(Pair(token, info))
        }

        override suspend fun update(input: UpdateUser): InteractionResult<UserInfo> {
            val (userId, username, email, password) = input.validate().bind()
            if (email != null || username != null) {
                return Failure(illegalState("Cannot update user with only null values"))
            }
            return persistence.update(userId, username, email, password)
        }

        override suspend fun getUser(userId: UserId): InteractionResult<UserInfo> {
            return persistence.select(userId)
        }

        override suspend fun getUser(username: String): InteractionResult<UserInfo> {
            return persistence.select(username)
        }
    }
