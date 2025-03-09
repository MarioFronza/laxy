package com.github.laxy.route

import arrow.core.raise.either
import com.github.laxy.auth.jwtAuth
import com.github.laxy.persistence.QuestionId
import com.github.laxy.persistence.QuizId
import com.github.laxy.persistence.SubjectId
import com.github.laxy.service.CreateQuiz
import com.github.laxy.service.JwtService
import com.github.laxy.service.QuizService
import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.resources.Resource
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.routing.Route
import kotlinx.serialization.Serializable

@Serializable data class QuizWrapper<T : Any>(val quiz: T)

@Serializable data class QuizzesWrapper<T : Any>(val quizzes: T)

@Serializable data class Quiz(val id: Long, val totalQuestions: Int)

@Serializable
data class QuizResponse(
    val id: Long,
    val subject: String,
    val totalQuestions: Int,
    val status: String,
    val createdAt: String
)

@Serializable
data class QuestionsResponse(
    val id: Long,
    val description: String,
    val options: List<OptionResponse>
)

@Serializable
data class OptionResponse(val id: Long, val description: String, val referenceNumber: Int)

@Serializable data class NewQuiz(val subjectId: Long, val totalQuestions: Int)

@Resource("/quizzes")
data class QuizzesResource(val parent: RootResource = RootResource) {
    @Resource("/{quizId}/questions")
    data class QuizQuestionsResource(
        val quizId: Long,
        val parent: QuizzesResource = QuizzesResource()
    ) {
        @Resource("/{questionId}/options")
        data class QuestionOptionsResource(
            val quizId: Long,
            val questionId: Long,
            val parent: QuizQuestionsResource = QuizQuestionsResource(quizId)
        )
    }
}

fun Route.quizRoutes(quizService: QuizService, jwtService: JwtService) {
    get<QuizzesResource> {
        jwtAuth(jwtService) { (_, userId) ->
            either {
                    val quizzes =
                        quizService.getByUser(userId).bind().map {
                            QuizResponse(
                                id = it.id.serial,
                                subject = it.subject,
                                totalQuestions = it.totalQuestions,
                                status = it.status,
                                createdAt = it.createdAt.toString()
                            )
                        }
                    QuizzesWrapper(quizzes)
                }
                .respond(this, OK)
        }
    }

    get<QuizzesResource.QuizQuestionsResource> { resource ->
        jwtAuth(jwtService) {
            either {
                    val questions = quizService.getQuestionsByQuiz(QuizId(resource.quizId)).bind()
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
                }
                .respond(this, OK)
        }
    }

    get<QuizzesResource.QuizQuestionsResource.QuestionOptionsResource> { resource ->
        jwtAuth(jwtService) {
            either {
                    val options =
                        quizService.getOptionsByQuestion(QuestionId(resource.questionId)).bind()
                    options.map {
                        OptionResponse(
                            id = it.id.serial,
                            description = it.description,
                            referenceNumber = it.referenceNumber
                        )
                    }
                }
                .respond(this, OK)
        }
    }

    post<QuizzesResource> {
        jwtAuth(jwtService) { (_, userId) ->
            either {
                    val (subjectId, totalQuestions) =
                        receiveCatching<QuizWrapper<NewQuiz>>().bind().quiz
                    val quiz =
                        quizService
                            .createQuiz(
                                CreateQuiz(
                                    userId = userId,
                                    subjectId = SubjectId(subjectId),
                                    totalQuestions
                                )
                            )
                            .bind()
                    QuizWrapper(quiz)
                }
                .respond(this, Created)
        }
    }
}
