package com.github.laxy.domain.auth

import com.github.laxy.persistence.UserId
import io.ktor.server.application.ApplicationCall
import io.ktor.util.pipeline.PipelineContext

@JvmInline
value class JwtToken(val value: String)

data class JwtContext(val token: JwtToken, val userId: UserId)

suspend inline fun PipelineContext<Unit, ApplicationCall>.optionalJwtAuth() {

}