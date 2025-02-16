package com.github.laxy.env

import arrow.fx.coroutines.continuations.ResourceScope
import com.github.laxy.persistence.quizPersistence
import com.github.laxy.persistence.subjectPersistence
import com.github.laxy.persistence.userPersistence
import com.github.laxy.service.JwtService
import com.github.laxy.service.QuizService
import com.github.laxy.service.UserService
import com.github.laxy.service.gptAIService
import com.github.laxy.service.jwtService
import com.github.laxy.service.quizService
import com.github.laxy.service.userService
import com.sksamuel.cohort.HealthCheckRegistry
import com.sksamuel.cohort.hikari.HikariConnectionsHealthCheck
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers

class Dependencies(
    val healthCheck: HealthCheckRegistry,
    val userService: UserService,
    val quizService: QuizService,
    val jwtService: JwtService
)

suspend fun ResourceScope.dependencies(env: Env): Dependencies {
    val hikari = hikari(env.dataSource)
    val openAI = env.openAI
    val sqlDelight = sqlDelight(hikari)
    val userPersistence = userPersistence(sqlDelight.usersQueries, sqlDelight.userThemesQueries)
    val subjectPersistence = subjectPersistence(sqlDelight.subjectsQueries)
    val quizPersistence =
        quizPersistence(
            sqlDelight.quizzesQueries,
            sqlDelight.questionsQueries,
            sqlDelight.questionOptionsQueries
        )
    val jwtService = jwtService(env.auth, userPersistence)
    val userService = userService(userPersistence, jwtService)
    val gptAIService = gptAIService(openAI.token)
    val quizService =
        quizService(userPersistence, subjectPersistence, quizPersistence, gptAIService)
    quizService.listenEvent()
    val checks =
        HealthCheckRegistry(Dispatchers.Default) {
            register(
                check = HikariConnectionsHealthCheck(hikari, 1),
                initialDelay = 1.seconds,
                checkInterval = 5.seconds
            )
        }
    return Dependencies(
        healthCheck = checks,
        userService = userService,
        jwtService = jwtService,
        quizService = quizService
    )
}
