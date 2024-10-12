package com.github.laxy.persistence

import com.github.laxy.service.UserInfo
import com.github.laxy.shared.DomainError
import com.github.laxy.shared.Failure
import com.github.laxy.shared.InteractionResult
import com.github.laxy.shared.Success
import com.github.laxy.shared.UsernameAlreadyExists
import com.github.laxy.sqldelight.UsersQueries
import org.postgresql.util.PSQLException
import org.postgresql.util.PSQLState

@JvmInline
value class UserId(val serial: Long)

interface UserPersistence {
    suspend fun insert(username: String, email: String): InteractionResult<DomainError, UserId>

    suspend fun select(userId: UserId): InteractionResult<DomainError, UserInfo>

    suspend fun select(username: String): InteractionResult<DomainError, UserId>

    suspend fun update(
        userId: UserId,
        email: String?,
        username: String?
    ): InteractionResult<DomainError, UserInfo>
}

fun userPersistence(
    usersQueries: UsersQueries
) =
    object : UserPersistence {
        override suspend fun insert(username: String, email: String): InteractionResult<DomainError, UserId> {
            return try {
                Success(usersQueries.create(username, email))
            } catch (e: PSQLException) {
                if (e.sqlState == PSQLState.UNIQUE_VIOLATION.state)
                    Failure(UsernameAlreadyExists(username))
                else throw e
            }
        }

        override suspend fun select(userId: UserId): InteractionResult<DomainError, UserInfo> {
            TODO("Not yet implemented")
        }

        override suspend fun select(username: String): InteractionResult<DomainError, UserId> {
            TODO("Not yet implemented")
        }

        override suspend fun update(
            userId: UserId,
            email: String?,
            username: String?
        ): InteractionResult<DomainError, UserInfo> {
            TODO("Not yet implemented")
        }

    }

private fun UsersQueries.create(
    username: String,
    email: String
): UserId =
    insertAndGetId(
        username = username,
        email = email
    ).executeAsOne()