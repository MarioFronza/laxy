package com.github.laxy.auth

import com.github.laxy.persistence.UserId
import com.github.laxy.route.respond
import com.github.laxy.service.JwtService
import io.ktor.http.HttpStatusCode.Companion.Unauthorized
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.parseAuthorizationHeader
import io.ktor.server.response.respond
import io.ktor.util.pipeline.PipelineContext

@JvmInline
value class JwtToken(val value: String)

data class JwtContext(val token: JwtToken, val userId: UserId)

suspend inline fun PipelineContext<Unit, ApplicationCall>.jwtAuth(
    jwtService: JwtService,
    crossinline body: suspend PipelineContext<Unit, ApplicationCall>.(JwtContext) -> Unit
) {
    optionalJwtAuth(jwtService) { jwtContext ->
        jwtContext?.let { body(this, it) } ?: call.respond(Unauthorized)
    }
}

suspend inline fun PipelineContext<Unit, ApplicationCall>.optionalJwtAuth(
    jwtService: JwtService,
    crossinline body: suspend PipelineContext<Unit, ApplicationCall>.(JwtContext?) -> Unit
) {
    jwtToken()?.let { token ->
        jwtService
            .verifyJwtToken(JwtToken(token))
            .fold(
                { error -> respond(error) },
                { userId -> body(this, JwtContext(JwtToken(token), userId)) }
            )
    } ?: body(this, null)
}

fun PipelineContext<Unit, ApplicationCall>.jwtToken(): String? {
    val header = call.request.parseAuthorizationHeader() as? HttpAuthHeader.Single
    return header?.blob
}
