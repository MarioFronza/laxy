package com.github.laxy.persistence

import com.github.laxy.domain.user.UserInfo
import com.github.laxy.domain.validation.DomainError
import com.github.laxy.domain.validation.UsernameAlreadyExists
import com.github.laxy.shared.Failure
import com.github.laxy.shared.InteractionResult
import com.github.laxy.shared.Success
import com.github.laxy.sqldelight.UsersQueries
import java.util.UUID.randomUUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import org.postgresql.util.PSQLException
import org.postgresql.util.PSQLState

@JvmInline value class UserId(val serial: Long)

interface UserPersistence {
    suspend fun insert(
        username: String,
        email: String,
        password: String
    ): InteractionResult<DomainError, UserId>

    suspend fun verifyPassword(
        email: String,
        password: String
    ): InteractionResult<DomainError, Pair<UserId, UserInfo>>

    suspend fun select(userId: UserId): InteractionResult<DomainError, UserInfo>

    suspend fun select(username: String): InteractionResult<DomainError, UserId>

    suspend fun update(
        userId: UserId,
        email: String?,
        username: String?,
        password: String?
    ): InteractionResult<DomainError, UserInfo>
}

fun userPersistence(
    usersQueries: UsersQueries,
    defaultIterations: Int = 64000,
    defaultKeyLength: Int = 512,
    secretKeyFactory: SecretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
) =
    object : UserPersistence {
        override suspend fun insert(
            username: String,
            email: String,
            password: String
        ): InteractionResult<DomainError, UserId> {
            val salt = generateSalt()
            val key = generateKey(password, salt)
            return try {
                Success(usersQueries.create(username, email, salt, key))
            } catch (e: PSQLException) {
                if (e.sqlState == PSQLState.UNIQUE_VIOLATION.state)
                    Failure(UsernameAlreadyExists(username))
                else throw e
            }
        }

        override suspend fun verifyPassword(
            email: String,
            password: String
        ): InteractionResult<DomainError, Pair<UserId, UserInfo>> {
            TODO("Not yet implemented")
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
            username: String?,
            password: String?
        ): InteractionResult<DomainError, UserInfo> {
            TODO("Not yet implemented")
        }

        private fun generateSalt(): ByteArray = randomUUID().toString().toByteArray()

        private fun generateKey(password: String, salt: ByteArray): ByteArray {
            val spec = PBEKeySpec(password.toCharArray(), salt, defaultIterations, defaultKeyLength)
            return secretKeyFactory.generateSecret(spec).encoded
        }
    }

private fun UsersQueries.create(
    username: String,
    email: String,
    salt: ByteArray,
    key: ByteArray
): UserId =
    insertAndGetId(username = username, email = email, salt = salt, hashed_password = key)
        .executeAsOne()
