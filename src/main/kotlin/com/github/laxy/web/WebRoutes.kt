package com.github.laxy.web

import arrow.core.raise.either
import com.github.laxy.persistence.UserId
import com.github.laxy.route.QuizResponse
import com.github.laxy.route.Subject
import com.github.laxy.service.Login
import com.github.laxy.service.QuizService
import com.github.laxy.service.RegisterUser
import com.github.laxy.service.SubjectService
import com.github.laxy.service.UserService
import com.github.laxy.util.toBrazilianFormat
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Principal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.http.content.staticResources
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import io.ktor.server.thymeleaf.Thymeleaf
import io.ktor.server.thymeleaf.ThymeleafContent
import kotlinx.serialization.Serializable
import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import org.thymeleaf.templateresolver.FileTemplateResolver

@Serializable data class UserSession(val token: String)

data class CurrentUserId(val userId: UserId) : Principal

fun Application.configureTemplating(
    userService: UserService,
    quizService: QuizService,
    subjectService: SubjectService
) {
    install(Thymeleaf) {
        setTemplateResolver(
            (if (developmentMode) {
                    FileTemplateResolver().apply {
                        cacheManager = null
                        prefix = "src/main/resources/templates/"
                    }
                } else {
                    ClassLoaderTemplateResolver().apply { prefix = "templates/" }
                })
                .apply {
                    suffix = ".html"
                    characterEncoding = "utf-8"
                    addDialect(LayoutDialect())
                }
        )
    }
    routing {
        staticResources("/static", "static")

        get("/") { call.respond(ThymeleafContent("index", emptyMap())) }

        get("/signin") {
            if (call.sessions.get<UserSession>() != null) {
                call.respondRedirect("/dashboard")
            } else {
                call.respond(ThymeleafContent("signin", emptyMap()))
            }
        }

        post("/signin") {
            val params = call.receiveParameters()
            val email = params["email"] ?: ""
            val password = params["password"] ?: ""

            either {
                    val (token, _) = userService.login(Login(email, password)).bind()
                    call.sessions.set(UserSession(token.value))
                    call.respondRedirect("/dashboard")
                }
                .mapLeft {
                    call.respond(
                        ThymeleafContent("/signin", mapOf("error" to "Invalid credentials"))
                    )
                }
        }

        get("/signup") {
            if (call.sessions.get<UserSession>() != null) {
                call.respondRedirect("/dashboard")
            } else {
                call.respond(ThymeleafContent("signup", emptyMap()))
            }
        }

        post("/signup") {
            val params = call.receiveParameters()
            val username = params["username"] ?: ""
            val email = params["email"] ?: ""
            val password = params["password"] ?: ""

            either {
                    val token =
                        userService.register(RegisterUser(username, email, password)).bind().value
                    call.sessions.set(UserSession(token))
                    call.respondRedirect("/dashboard")
                }
                .mapLeft {
                    call.respond(
                        ThymeleafContent("signup", mapOf("error" to "Registration failed"))
                    )
                }
        }

        get("/signout") {
            call.sessions.clear<UserSession>()
            call.respondRedirect("/signin")
        }

        authenticate("auth-session") {
            get("/dashboard") {
                val current = call.principal<CurrentUserId>()
                if (current != null) {
                    either {
                            val quizzes =
                                quizService.getByUser(current.userId).bind().map {
                                    QuizResponse(
                                        id = it.id.serial,
                                        subject = it.subject,
                                        totalQuestions = it.totalQuestions,
                                        status = it.status,
                                        createdAt = it.createdAt.toBrazilianFormat()
                                    )
                                }
                            call.respond(ThymeleafContent("dashboard", mapOf("quizzes" to quizzes)))
                        }
                        .mapLeft { call.respond(ThymeleafContent("dashboard", emptyMap())) }
                } else {
                    call.respondRedirect("/signin")
                }
            }

            get("/create-quiz") {
                val current = call.principal<CurrentUserId>()
                if (current != null) {
                    either {
                            val subjects =
                                subjectService.getAllSubjects().bind().map {
                                    Subject(
                                        id = it.id.serial,
                                        name = it.name,
                                        description = it.description,
                                        language = it.language,
                                    )
                                }
                            call.respond(
                                ThymeleafContent("create-quiz", mapOf("subjects" to subjects))
                            )
                        }
                        .mapLeft { call.respondRedirect("/dashboard") }
                } else {
                    call.respondRedirect("/signin")
                }
            }
        }
    }
}
