package com.github.laxy.service

import com.github.laxy.persistence.UserId
import com.github.laxy.shared.InteractionResult
import com.github.laxy.usecase.validation.DomainError

data class RegisterUser(val username: String, val email: String)

data class UpdateUser(val userId: UserId, val username: String?, val email: String?)

data class UserInfo(val username: String, val email: String)

interface UserService {
    suspend fun register(input: RegisterUser): InteractionResult<DomainError, UserInfo>

    suspend fun update(input: UpdateUser): InteractionResult<DomainError, UserInfo>

    suspend fun getUser(userId: UserId): InteractionResult<DomainError, UserInfo>

    suspend fun getUser(username: String): InteractionResult<DomainError, UserInfo>
}
