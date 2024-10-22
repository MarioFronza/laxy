package com.github.laxy.domain.user

import com.github.laxy.domain.validation.DomainError
import com.github.laxy.domain.validation.IncorrectInput
import com.github.laxy.persistence.UserId
import com.github.laxy.persistence.UserPersistence
import com.github.laxy.shared.Failure
import com.github.laxy.shared.InteractionResult
import com.github.laxy.shared.Success

data class RegisterUser(val username: String, val email: String)

data class UpdateUser(val userId: UserId, val username: String?, val email: String?)

data class UserInfo(val username: String, val email: String)

interface UserService {
    suspend fun register(input: RegisterUser): InteractionResult<DomainError, UserInfo>

    suspend fun update(input: UpdateUser): InteractionResult<DomainError, UserInfo>

    suspend fun getUser(userId: UserId): InteractionResult<DomainError, UserInfo>

    suspend fun getUser(username: String): InteractionResult<DomainError, UserInfo>
}

fun userService(persistence: UserPersistence) =
    object : UserService {
        override suspend fun register(input: RegisterUser): InteractionResult<DomainError, UserInfo> {
            val validationResponse = input.validate()
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