package com.github.laxy.domain.user

import com.github.laxy.domain.auth.JwtToken
import com.github.laxy.domain.validation.DomainError
import com.github.laxy.persistence.UserId
import com.github.laxy.persistence.UserPersistence
import com.github.laxy.shared.InteractionResult
import com.github.laxy.shared.Success
import com.github.laxy.shared.interaction

data class RegisterUser(val username: String, val email: String, val password: String)

data class UpdateUser(val userId: UserId, val username: String?, val email: String?)

data class Login(val email: String, val password: String)

data class UserInfo(val username: String, val email: String)

interface UserService {
    suspend fun register(input: RegisterUser): InteractionResult<DomainError, UserId>

    suspend fun login(input: Login): InteractionResult<DomainError, Pair<JwtToken, UserInfo>>

    suspend fun update(input: UpdateUser): InteractionResult<DomainError, UserInfo>

    suspend fun getUser(userId: UserId): InteractionResult<DomainError, UserInfo>

    suspend fun getUser(username: String): InteractionResult<DomainError, UserInfo>
}

fun userService(persistence: UserPersistence) =
    object : UserService {
        override suspend fun register(input: RegisterUser): InteractionResult<DomainError, UserId> =
            interaction {
                val (username, email, password) = input.validate().bind()
                val userId = persistence.insert(username, email, password).bind()
                return Success(userId)
            }

        override suspend fun login(input: Login): InteractionResult<DomainError, Pair<JwtToken, UserInfo>>  = interaction {
        }

        override suspend fun update(input: UpdateUser): InteractionResult<DomainError, UserInfo> {
            TODO("Not yet implemented")
        }

        override suspend fun getUser(userId: UserId): InteractionResult<DomainError, UserInfo> {
            TODO("Not yet implemented")
        }

        override suspend fun getUser(username: String): InteractionResult<DomainError, UserInfo> {
            TODO("Not yet implemented")
        }
    }
