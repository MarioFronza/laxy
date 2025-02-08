package com.github.laxy.route

import arrow.core.raise.either
import com.github.laxy.auth.jwtAuth
import com.github.laxy.persistence.SubjectId
import com.github.laxy.service.CreateQuiz
import com.github.laxy.service.JwtService
import com.github.laxy.service.QuizService
import io.ktor.http.HttpStatusCode
import io.ktor.resources.Resource
import io.ktor.server.resources.post
import io.ktor.server.routing.Route
import kotlinx.serialization.Serializable

@Serializable
data class QuizWrapper<T : Any>(val quiz: T)

@Serializable
data class Quiz(val id: Long, val totalQuestions: Int)

@Serializable
data class NewQuiz(val subjectId: Long, val totalQuestions: Int)

@Resource("/quizzes")
data class QuizzesResource(val parent: RootResource = RootResource)

fun Route.quizRoutes(quizService: QuizService, jwtService: JwtService) {
    post<QuizzesResource> {
        jwtAuth(jwtService) { (_, userId) ->
            either {
                val (subjectId, totalQuestions) =
                    receiveCatching<QuizWrapper<NewQuiz>>().bind().quiz
                val quiz = quizService.createQuiz(
                    CreateQuiz(
                        userId = userId,
                        subjectId = SubjectId(subjectId),
                        totalQuestions
                    )
                )
                QuizWrapper(quiz)
            }
                .respond(HttpStatusCode.Created)
        }

    }
}
