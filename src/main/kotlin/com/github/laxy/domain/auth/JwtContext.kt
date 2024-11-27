package com.github.laxy.domain.auth

import com.github.laxy.persistence.UserId
import com.github.laxy.shared.Failure
import com.github.laxy.shared.Success
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.parseAuthorizationHeader
import io.ktor.util.pipeline.PipelineContext

@JvmInline
value class JwtToken(val value: String)

data class JwtContext(val token: JwtToken, val userId: UserId)

suspend inline fun PipelineContext<Unit, ApplicationCall>.jwtAuth(
    jwtService: JwtService,
    body: PipelineContext<Unit, ApplicationCall>.(JwtContext) -> Unit
) {
    optionalJwtAuth()
}

suspend inline fun PipelineContext<Unit, ApplicationCall>.optionalJwtAuth(
    jwtService: JwtService,
    body: PipelineContext<Unit, ApplicationCall>.(JwtContext?) -> Unit
) {
    jwtToken()?.let { token ->
        val interactionResponse = jwtService.verifyJwtToken(JwtToken(token))
        when (interactionResponse) {
            is Failure -> respond(error)
            is Success -> body(this, JwtContext(JwtToken(token), userId))
        }
    } ?: body(this, null)
}

fun PipelineContext<Unit, ApplicationCall>.jwtToken(): String? {
    val header = call.request.parseAuthorizationHeader() as? HttpAuthHeader.Single
    return header?.blob
}

