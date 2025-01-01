package com.github.laxy.domain.auth

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import com.github.laxy.env.Env
import com.github.laxy.persistence.UserId
import com.github.laxy.persistence.UserPersistence
import com.github.laxy.shared.Failure
import com.github.laxy.shared.IllegalStateError
import com.github.laxy.shared.IllegalStateError.Companion.illegalState
import com.github.laxy.shared.InteractionResult
import com.github.laxy.shared.Success
import com.github.laxy.shared.interaction
import io.github.nefilim.kjwt.JWSAlgorithm
import io.github.nefilim.kjwt.JWSES512Algorithm
import io.github.nefilim.kjwt.JWT
import io.github.nefilim.kjwt.KJWTSignError
import io.github.nefilim.kjwt.KJWTSignError.InvalidJWTData
import io.github.nefilim.kjwt.KJWTSignError.InvalidKey
import io.github.nefilim.kjwt.KJWTSignError.SigningError
import io.github.nefilim.kjwt.SignedJWT
import io.github.nefilim.kjwt.sign
import java.time.Clock
import java.time.Instant
import kotlin.time.toJavaDuration

interface JwtService {
    suspend fun generateJwtToken(userId: UserId): InteractionResult<JwtToken>

    suspend fun verifyJwtToken(token: JwtToken): InteractionResult<UserId>
}

fun jwtService(env: Env.Auth, persistence: UserPersistence) =
    object : JwtService {
        override suspend fun generateJwtToken(userId: UserId): InteractionResult<JwtToken> {
            val jwt =
                JWT.hs512 {
                        val now = Instant.now(Clock.systemUTC())
                        issuedAt(now)
                        expiresAt(now + env.duration.toJavaDuration())
                        issuer(env.issuer)
                        claim("id", userId.serial)
                    }
                    .sign(env.secret)
                    .toUserServiceError()
                    .map { JwtToken(it.rendered) }

            return when (jwt) {
                is Left -> Failure(jwt.value)
                is Right -> Success(jwt.value)
            }
        }

        override suspend fun verifyJwtToken(token: JwtToken): InteractionResult<UserId> =
            interaction {
                return when (val decodeResponse = JWT.decodeT(token.value, JWSES512Algorithm)) {
                    is Left -> Failure(illegalState("Invalid JWT"))
                    is Right -> {
                        val userId =
                            decodeResponse.value.claimValueAsLong("id").orNull()
                                ?: return Failure(illegalState("id missing from JWT Token"))
                        val expiresAt = decodeResponse.value.expiresAt().orNull()
                        if (
                            expiresAt == null || expiresAt.isAfter(Instant.now(Clock.systemUTC()))
                        ) {
                            return Failure(illegalState("JWT Token expired"))
                        }
                        persistence.select(UserId(userId)).bind()
                        Success(UserId(userId))
                    }
                }
            }
    }

private fun <A : JWSAlgorithm> Either<KJWTSignError, SignedJWT<A>>.toUserServiceError():
    Either<IllegalStateError, SignedJWT<A>> = mapLeft { jwtError ->
    when (jwtError) {
        InvalidKey -> illegalState("JWT singing error: invalid Secret Key.")
        InvalidJWTData -> illegalState("JWT singing error: Generated with incorrect JWT data")
        is SigningError -> illegalState("JWT singing error: ${jwtError.cause}")
    }
}
