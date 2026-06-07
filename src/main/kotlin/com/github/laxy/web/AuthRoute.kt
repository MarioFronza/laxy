package com.github.laxy.web

import arrow.core.raise.either
import com.github.laxy.service.Login
import com.github.laxy.service.RegisterUser
import com.github.laxy.service.UserService
import io.ktor.server.application.call
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set

fun Route.authRoutes(userService: UserService) {
    get("/") {
        if (call.sessions.get<UserSession>() != null) {
            call.respondRedirect("/dashboard")
        } else {
            call.respondTemplateWithFlash("index")
        }
    }

    get("/signin") {
        if (call.sessions.get<UserSession>() != null) {
            call.respondRedirect("/dashboard")
        } else {
            call.respondTemplateWithFlash("signin")
        }
    }

    post("/signin") {
        val params = call.receiveParameters()
        val email = params["email"].orEmpty()
        val password = params["password"].orEmpty()

        either {
                val (token, _) = userService.login(Login(email, password)).bind()
                call.sessions.set(UserSession(token.value))
                call.successRedirect("/dashboard", "Signed in successfully.")
            }
            .mapLeft { error -> call.respondTemplate("signin", message = error.toPageMessage()) }
    }

    get("/signup") {
        if (call.sessions.get<UserSession>() != null) {
            call.respondRedirect("/dashboard")
        } else {
            call.respondTemplateWithFlash("signup")
        }
    }

    post("/signup") {
        val params = call.receiveParameters()
        val username = params["username"].orEmpty()
        val email = params["email"].orEmpty()
        val password = params["password"].orEmpty()

        either {
                val token =
                    userService.register(RegisterUser(username, email, password)).bind().value
                call.sessions.set(UserSession(token))
                call.successRedirect("/dashboard", "Account created successfully.")
            }
            .mapLeft { error -> call.respondTemplate("signup", message = error.toPageMessage()) }
    }

    get("/signout") {
        call.sessions.clear<UserSession>()
        call.successRedirect("/signin", "You have been signed out.")
    }
}
