package com.github.laxy.domain.user

import com.github.laxy.domain.validation.DomainError
import com.github.laxy.domain.validation.IncorrectInput
import com.github.laxy.persistence.UserId
import com.github.laxy.persistence.UserPersistence
import com.github.laxy.shared.Failure
import com.github.laxy.shared.InteractionResult
import com.github.laxy.shared.Success
import com.github.laxy.shared.either

data class RegisterUser(val username: String, val email: String, val password: String)

data class UpdateUser(val userId: UserId, val username: String?, val email: String?)

data class UserInfo(val username: String, val email: String)

interface UserService {
    suspend fun register(input: RegisterUser): InteractionResult<DomainError, UserId>

    suspend fun update(input: UpdateUser): InteractionResult<DomainError, UserInfo>

    suspend fun getUser(userId: UserId): InteractionResult<DomainError, UserInfo>

    suspend fun getUser(username: String): InteractionResult<DomainError, UserInfo>
}

fun userService(persistence: UserPersistence) =
    object : UserService {
        override suspend fun register(input: RegisterUser): InteractionResult<DomainError, UserId> {
            when (val validationResponse = input.validate()) {
                is Failure -> return Failure(validationResponse.error)
                is Success -> {
                    val (username, email, password) = validationResponse.data
                    return when (val persistenceResponse = persistence.insert(username, email, password)) {
                        is Failure -> Failure(persistenceResponse.error)
                        is Success -> Success(persistenceResponse.data)
                    }
                }

            }
        }

        override suspend fun update(input: UpdateUser): InteractionResult<DomainError, UserInfo> = either {
            TODO("Not yet implemented")
        }

        override suspend fun getUser(userId: UserId): InteractionResult<DomainError, UserInfo> {
            TODO("Not yet implemented")
        }

        override suspend fun getUser(username: String): InteractionResult<DomainError, UserInfo> {
            TODO("Not yet implemented")
        }
    }