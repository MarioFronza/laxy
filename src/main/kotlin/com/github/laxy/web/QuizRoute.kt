package com.github.laxy.web

import arrow.core.raise.either
import com.github.laxy.persistence.QuestionOptionId
import com.github.laxy.persistence.QuizId
import com.github.laxy.persistence.SubjectId
import com.github.laxy.route.OptionResponse
import com.github.laxy.route.QuestionAttemptResponse
import com.github.laxy.route.QuestionsResponse
import com.github.laxy.route.QuizResponse
import com.github.laxy.route.Subject
import com.github.laxy.service.CreateQuiz
import com.github.laxy.service.CreateTheme
import com.github.laxy.service.QuestionAttempt
import com.github.laxy.service.QuizAttempt
import com.github.laxy.service.QuizService
import com.github.laxy.service.SubjectService
import com.github.laxy.service.UserService
import com.github.laxy.util.toBrazilianFormat
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receiveParameters
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

@Suppress("LongMethod")
fun Route.quizRoutes(
    quizService: QuizService,
    subjectService: SubjectService,
    userService: UserService,
) {
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
                    call.respondTemplate(
                        "dashboard",
                        mapOf("quizzes" to quizzes),
                        call.consumeFlashMessage()
                    )
                }
                .mapLeft { call.respondTemplate("dashboard", message = call.consumeFlashMessage()) }
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
                    call.respondTemplate(
                        "create-quiz",
                        mapOf("subjects" to subjects),
                        call.consumeFlashMessage()
                    )
                }
                .mapLeft { call.errorRedirect("/dashboard", "Could not load subjects.") }
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

                    call.successRedirect("/dashboard", "Quiz created successfully.")
                }
                .mapLeft { error -> call.redirectWithFlash("/quizzes", error.toPageMessage()) }
        }

        get("/quizzes/{id}/questions") {
            call.currentUserOrRedirect() ?: return@get
            val quizId = call.parameters["id"].orEmpty()

            either {
                    val questions = quizService.getQuestionsByQuiz(QuizId(quizId.toLong())).bind()
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
                                    },
                                lastAttempt =
                                    it.lastAttempt?.let { attempt ->
                                        QuestionAttemptResponse(
                                            selectedOptionId = attempt.selectedOptionId.serial,
                                            isCorrect = attempt.isCorrect
                                        )
                                    }
                            )
                        }

                    call.respondTemplate(
                        "questions",
                        mapOf("questions" to response, "quizId" to quizId),
                        call.consumeFlashMessage()
                    )
                }
                .mapLeft { call.errorRedirect("/dashboard", "Failed to load quiz questions.") }
        }

        post("/quizzes/{id}/attempt") {
            call.currentUserOrRedirect() ?: return@post
            val quizId = call.parameters["id"]?.toLongOrNull()
            if (quizId == null) {
                call.errorRedirect("/dashboard", "Invalid quiz ID.")
                return@post
            }

            val params = call.receiveParameters()

            either {
                    val questions = quizService.getQuestionsByQuiz(QuizId(quizId)).bind()

                    val attempts =
                        questions.mapNotNull { question ->
                            val selectedOptionId =
                                params["option__${question.id.serial}"]?.toLongOrNull()
                            selectedOptionId?.let {
                                QuestionAttempt(
                                    id = question.id,
                                    selectedOptionId = QuestionOptionId(it),
                                    isCorrect = false
                                )
                            }
                        }

                    if (attempts.isEmpty()) {
                        call.errorRedirect(
                            "/quizzes/$quizId/questions",
                            "Please select all options."
                        )
                        return@either Unit
                    }

                    quizService.quizAttempt(QuizAttempt(QuizId(quizId), attempts)).bind()
                    call.successRedirect(
                        "/quizzes/$quizId/questions",
                        "Your answers have been submitted."
                    )
                }
                .mapLeft { error ->
                    call.redirectWithFlash("/quizzes/$quizId/questions", error.toPageMessage())
                }
        }

        post("/quizzes/{id}") {
            call.currentUserOrRedirect() ?: return@post
            val quizId = call.parameters["id"].orEmpty()
            quizService.deleteById(QuizId(quizId.toLong()))
            call.successRedirect("/dashboard", "Quiz deleted successfully.")
        }
    }
}
