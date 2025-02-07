package com.github.laxy.persistence

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.github.laxy.DomainError
import com.github.laxy.PasswordNotMatched
import com.github.laxy.UserNotFound
import com.github.laxy.UsernameAlreadyExists
import com.github.laxy.service.UserInfo
import com.github.laxy.service.UserThemeInfo
import com.github.laxy.sqldelight.UserThemesQueries
import com.github.laxy.sqldelight.UsersQueries
import org.postgresql.util.PSQLException
import org.postgresql.util.PSQLState.UNIQUE_VIOLATION
import java.util.UUID.randomUUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

@JvmInline
value class UserId(val serial: Long)

interface UserPersistence {
    suspend fun insert(
        username: String,
        email: String,
        password: String
    ): Either<DomainError, UserId>

    suspend fun insertTheme(
        userId: UserId,
        description: String
    ): Either<DomainError, UserThemeInfo>

    suspend fun verifyPassword(
        email: String,
        password: String
    ): Either<DomainError, Pair<UserId, UserInfo>>

    suspend fun select(userId: UserId): Either<DomainError, UserInfo>

    suspend fun select(username: String): Either<DomainError, UserInfo>

    suspend fun update(
        userId: UserId,
        username: String?,
        email: String?,
        password: String?
    ): Either<DomainError, UserInfo>
}

fun userPersistence(
    usersQueries: UsersQueries,
    userThemesQueries: UserThemesQueries,
    defaultIterations: Int = 64000,
    defaultKeyLength: Int = 512,
    secretKeyFactory: SecretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
) =
    object : UserPersistence {
        override suspend fun insert(
            username: String,
            email: String,
            password: String
        ): Either<DomainError, UserId> {
            val salt = generateSalt()
            val key = generateKey(password, salt)
            return Either.catchOrThrow<PSQLException, UserId> {
                usersQueries.create(username, email, salt, key)
            }
                .mapLeft { psqlException ->
                    if (psqlException.sqlState == UNIQUE_VIOLATION.state)
                        UsernameAlreadyExists(username)
                    else throw psqlException
                }
        }

        override suspend fun insertTheme(userId: UserId, description: String): Either<DomainError, UserThemeInfo> =
            either {
                val userInfo = usersQueries.selectById(userId) { email, username, _, _ -> UserInfo(username, email) }
                ensureNotNull(userInfo) { UserNotFound("userId=$userId") }
                UserThemeInfo(
                    description = userThemesQueries.insertAndGetDescription(userId.serial, description, true)
                        .executeAsOne()
                )
            }

        override suspend fun verifyPassword(
            email: String,
            password: String
        ): Either<DomainError, Pair<UserId, UserInfo>> = either {
            val (userId, username, salt, key) =
                ensureNotNull(usersQueries.selectSecurityByEmail(email).executeAsOneOrNull()) {
                    UserNotFound("email=$email")
                }
            val hash = generateKey(password, salt)
            ensure(hash contentEquals key) { PasswordNotMatched }
            Pair(userId, UserInfo(username, email))
        }

        override suspend fun select(userId: UserId): Either<DomainError, UserInfo> = either {
            val userInfo =
                usersQueries
                    .selectById(userId) { email, username, _, _ -> UserInfo(username, email) }
                    .executeAsOneOrNull()
            ensureNotNull(userInfo) { UserNotFound("userId=$userId") }
        }

        override suspend fun select(username: String): Either<DomainError, UserInfo> = either {
            val userInfo = usersQueries.selectByUsername(username, ::UserInfo).executeAsOneOrNull()
            ensureNotNull(userInfo) { UserNotFound("username=$username") }
        }

        override suspend fun update(
            userId: UserId,
            username: String?,
            email: String?,
            password: String?
        ): Either<DomainError, UserInfo> = either {
            val info =
                usersQueries.transactionWithResult {
                    usersQueries.selectById(userId).executeAsOneOrNull()
                        ?.let { (oldEmail, oldUsername, salt, oldPassword) ->
                            val newPassword = password?.let { generateKey(it, salt) } ?: oldPassword
                            val newEmail = email ?: oldEmail
                            val newUsername = username ?: oldUsername
                            usersQueries.update(newEmail, newUsername, newPassword, userId)
                            UserInfo(newUsername, newEmail)
                        }
                }
            ensureNotNull(info) { UserNotFound("userId=$userId") }
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
