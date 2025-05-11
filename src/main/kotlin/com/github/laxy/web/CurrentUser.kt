package com.github.laxy.web

import com.github.laxy.persistence.UserId
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.Principal
import io.ktor.server.auth.principal
import io.ktor.server.response.respondRedirect
import kotlinx.serialization.Serializable

@Serializable data class UserSession(val token: String)

data class CurrentUserId(val userId: UserId) : Principal

suspend fun ApplicationCall.currentUserOrRedirect(): CurrentUserId? {
    val current = this.principal<CurrentUserId>()
    if (current == null) this.respondRedirect("/signin")
    return current
}
