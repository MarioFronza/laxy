package com.github.laxy.persistence

import com.github.laxy.domain.user.UserInfo
import com.github.laxy.shared.Failure
import com.github.laxy.shared.IllegalStateError.Companion.illegalState
import com.github.laxy.shared.InteractionResult
import com.github.laxy.shared.Success
import com.github.laxy.shared.interaction
import com.github.laxy.sqldelight.UsersQueries
import java.util.UUID.randomUUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import org.postgresql.util.PSQLException
import org.postgresql.util.PSQLState

@JvmInline value class UserId(val serial: Long)

interface UserPersistence {
    suspend fun insert(username: String, email: String, password: String): InteractionResult<UserId>

    suspend fun verifyPassword(
        email: String,
        password: String
    ): InteractionResult<Pair<UserId, UserInfo>>

    suspend fun select(userId: UserId): InteractionResult<UserInfo>

    suspend fun select(username: String): InteractionResult<UserInfo>

    suspend fun update(
        userId: UserId,
        email: String?,
        username: String?,
        password: String?
    ): InteractionResult<UserInfo>
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
        ): InteractionResult<UserId> {
            val salt = generateSalt()
            val key = generateKey(password, salt)
            return try {
                Success(usersQueries.create(username, email, salt, key))
            } catch (e: PSQLException) {
                if (e.sqlState == PSQLState.UNIQUE_VIOLATION.state) Failure(illegalState(username))
                else throw e
            }
        }

        override suspend fun verifyPassword(
            email: String,
            password: String
        ): InteractionResult<Pair<UserId, UserInfo>> = interaction {
            val queryResponse =
                usersQueries.selectSecurityByEmail(email).executeAsOneOrNull()
                    ?: return Failure(illegalState("User not found for"))
            val (userId, username, salt, key) = queryResponse
            val hash = generateKey(password, salt)
            if (!key.contentEquals(hash)) {
                return Failure(illegalState("Password not matched"))
            }
            Success(Pair(userId, UserInfo(username, email)))
        }

        override suspend fun select(userId: UserId): InteractionResult<UserInfo> = interaction {
            val userInfo =
                usersQueries
                    .selectById(userId) { email, username, _, _ -> UserInfo(username, email) }
                    .executeAsOneOrNull()
            if (userInfo == null) {
                return Failure(illegalState("User not found"))
            }
            Success(userInfo)
        }

        override suspend fun select(username: String): InteractionResult<UserInfo> = interaction {
            val userInfo =
                usersQueries.selectByUsername(username, ::UserInfo).executeAsOneOrNull()
                    ?: return Failure(illegalState("User not found"))
            Success(userInfo)
        }

        override suspend fun update(
            userId: UserId,
            email: String?,
            username: String?,
            password: String?
        ): InteractionResult<UserInfo> = interaction {
            val info =
                usersQueries.transactionWithResult {
                    usersQueries.selectById(userId).executeAsOneOrNull()?.let {
                        (oldEmail, oldUsername, salt, oldPassword) ->
                        val newPassword = password?.let { generateKey(it, salt) } ?: oldPassword
                        val newEmail = email ?: oldEmail
                        val newUsername = username ?: oldUsername
                        usersQueries.update(newEmail, newUsername, newPassword, userId)
                        UserInfo(newUsername, newEmail)
                    }
                }
            if (info == null) {
                return Failure(illegalState("User not found"))
            }
            Success(info)
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
