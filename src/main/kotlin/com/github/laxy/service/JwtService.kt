package com.github.laxy.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.github.laxy.DomainError
import com.github.laxy.JwtGeneration
import com.github.laxy.JwtInvalid
import com.github.laxy.auth.JwtToken
import com.github.laxy.env.Env
import com.github.laxy.persistence.UserId
import com.github.laxy.persistence.UserPersistence
import io.github.nefilim.kjwt.JWSAlgorithm
import io.github.nefilim.kjwt.JWSHMAC512Algorithm
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
    suspend fun generateJwtToken(userId: UserId): Either<JwtGeneration, JwtToken>

    suspend fun verifyJwtToken(token: JwtToken): Either<DomainError, UserId>
}

fun jwtService(env: Env.Auth, persistence: UserPersistence) =
    object : JwtService {
        override suspend fun generateJwtToken(userId: UserId): Either<JwtGeneration, JwtToken> =
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

        override suspend fun verifyJwtToken(token: JwtToken): Either<DomainError, UserId> = either {
            val jwt =
                JWT.decodeT(token.value, JWSHMAC512Algorithm)
                    .mapLeft { JwtInvalid(it.toString()) }
                    .bind()
            val userId =
                ensureNotNull(jwt.claimValueAsLong("id").getOrNull()) {
                    JwtInvalid("id missing from JWT Token")
                }
            val expiresAt =
                ensureNotNull(jwt.expiresAt().getOrNull()) {
                    JwtInvalid("exp missing from JWT Token")
                }
            ensure(expiresAt.isAfter(Instant.now(Clock.systemUTC()))) {
                JwtInvalid("JWT Token expired")
            }
            persistence.select(UserId(userId)).bind()
            UserId(userId)
        }
    }

private fun <A : JWSAlgorithm> Either<KJWTSignError, SignedJWT<A>>.toUserServiceError():
        Either<JwtGeneration, SignedJWT<A>> = mapLeft { jwtError ->
    when (jwtError) {
        InvalidKey -> JwtGeneration("JWT singing error: invalid Secret Key.")
        InvalidJWTData -> JwtGeneration("JWT singing error: Generated with incorrect JWT data")
        is SigningError -> JwtGeneration("JWT singing error: ${jwtError.cause}")
    }
}
