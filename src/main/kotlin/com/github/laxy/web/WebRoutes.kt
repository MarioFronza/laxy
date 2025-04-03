package com.github.laxy.web

import arrow.core.raise.either
import com.github.laxy.persistence.QuizId
import com.github.laxy.persistence.SubjectId
import com.github.laxy.persistence.UserId
import com.github.laxy.route.OptionResponse
import com.github.laxy.route.QuestionsResponse
import com.github.laxy.route.QuizResponse
import com.github.laxy.route.Subject
import com.github.laxy.service.CreateQuiz
import com.github.laxy.service.CreateTheme
import com.github.laxy.service.Login
import com.github.laxy.service.QuizService
import com.github.laxy.service.RegisterUser
import com.github.laxy.service.SubjectService
import com.github.laxy.service.UserService
import com.github.laxy.util.toBrazilianFormat
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
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

@Serializable data class UserSession(val token: String)

data class CurrentUserId(val userId: UserId) : Principal

@Suppress("LongMethod")
fun Application.configureTemplating(
    userService: UserService,
    quizService: QuizService,
    subjectService: SubjectService
) {
    install(Thymeleaf) {
        setTemplateResolver(
            ClassLoaderTemplateResolver().apply {
                prefix = "templates/"
                suffix = ".html"
                characterEncoding = "utf-8"
                addDialect(LayoutDialect())
            }
        )
    }

    routing {
        staticResources("/static", "static")

        suspend fun ApplicationCall.currentUserOrRedirect(): CurrentUserId? {
            val current = this.principal<CurrentUserId>()
            if (current == null) {
                this.respondRedirect("/signin")
            }
            return current
        }

        fun respondTemplate(name: String, data: Map<String, Any> = emptyMap()) =
            ThymeleafContent(name, data)

        get("/") {
            if (call.sessions.get<UserSession>() != null) {
                call.respondRedirect("/dashboard")
            } else {
                call.respond(respondTemplate("index"))
            }
        }

        get("/signin") {
            if (call.sessions.get<UserSession>() != null) {
                call.respondRedirect("/dashboard")
            } else {
                call.respond(respondTemplate("signin"))
            }
        }

        post("/signin") {
            val params = call.receiveParameters()
            val email = params["email"].orEmpty()
            val password = params["password"].orEmpty()

            either {
                    val (token, _) = userService.login(Login(email, password)).bind()
                    call.sessions.set(UserSession(token.value))
                    call.respondRedirect("/dashboard")
                }
                .mapLeft {
                    call.respond(respondTemplate("signin", mapOf("error" to "Invalid credentials")))
                }
        }

        get("/signup") {
            if (call.sessions.get<UserSession>() != null) {
                call.respondRedirect("/dashboard")
            } else {
                call.respond(respondTemplate("signup"))
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
                    call.respondRedirect("/dashboard")
                }
                .mapLeft {
                    call.respond(respondTemplate("signup", mapOf("error" to "Registration failed")))
                }
        }

        get("/signout") {
            call.sessions.clear<UserSession>()
            call.respondRedirect("/signin")
        }

        authenticate("auth-session") {
            get("/dashboard") {
                val current = call.currentUserOrRedirect() ?: return@get
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
                        call.respond(respondTemplate("dashboard", mapOf("quizzes" to quizzes)))
                    }
                    .mapLeft { call.respond(respondTemplate("dashboard")) }
            }

            get("/quizzes") {
                call.currentUserOrRedirect() ?: return@get
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
                        call.respond(respondTemplate("create-quiz", mapOf("subjects" to subjects)))
                    }
                    .mapLeft { call.respondRedirect("/dashboard") }
            }

            post("/quizzes") {
                val current = call.currentUserOrRedirect() ?: return@post
                val params = call.receiveParameters()
                val subjectId = params["subjectId"].orEmpty()
                val theme = params["theme"].orEmpty()
                val totalQuestions = params["totalQuestions"].orEmpty()

                either {
                        userService
                            .createTheme(CreateTheme(userId = current.userId, description = theme))
                            .bind()

                        quizService.createQuiz(
                            CreateQuiz(
                                userId = current.userId,
                                subjectId = SubjectId(subjectId.toLong()),
                                totalQuestions = totalQuestions.toInt()
                            )
                        )

                        call.respondRedirect("/dashboard")
                    }
                    .mapLeft { call.respondRedirect("/quizzes") }
            }

            get("/quizzes/{id}/questions") {
                call.currentUserOrRedirect() ?: return@get
                val quizId = call.parameters["id"].orEmpty()

                either {
                        val questions =
                            quizService.getQuestionsByQuiz(QuizId(quizId.toLong())).bind()
                        val response =
                            questions.map {
                                QuestionsResponse(
                                    id = it.id.serial,
                                    description = it.description,
                                    options =
                                        it.options.map { option ->
                                            OptionResponse(
                                                id = option.id.serial,
                                                description = option.description,
                                                referenceNumber = option.referenceNumber
                                            )
                                        }
                                )
                            }
                        call.respond(respondTemplate("questions", mapOf("questions" to response)))
                    }
                    .mapLeft { call.respondRedirect("/dashboard") }
            }
        }
    }
}
