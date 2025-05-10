package com.github.laxy.env

import arrow.fx.coroutines.continuations.ResourceScope
import com.github.laxy.persistence.languagePersistence
import com.github.laxy.persistence.questionAttemptPersistence
import com.github.laxy.persistence.questionOptionsPersistence
import com.github.laxy.persistence.questionPersistence
import com.github.laxy.persistence.quizPersistence
import com.github.laxy.persistence.subjectPersistence
import com.github.laxy.persistence.userPersistence
import com.github.laxy.service.JwtService
import com.github.laxy.service.LanguageService
import com.github.laxy.service.QuizService
import com.github.laxy.service.SubjectService
import com.github.laxy.service.UserService
import com.github.laxy.service.gptAIService
import com.github.laxy.service.jwtService
import com.github.laxy.service.languageService
import com.github.laxy.service.quizService
import com.github.laxy.service.subjectService
import com.github.laxy.service.userService
import com.sksamuel.cohort.HealthCheckRegistry
import com.sksamuel.cohort.hikari.HikariConnectionsHealthCheck
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class Dependencies(
    val healthCheck: HealthCheckRegistry,
    val userService: UserService,
    val quizService: QuizService,
    val jwtService: JwtService,
    val subjectService: SubjectService,
    val languageService: LanguageService
)

suspend fun ResourceScope.dependencies(env: Env): Dependencies {
    val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    otel(env.otel)
    val hikari = hikari(env.dataSource)
    val openAI = env.openAI
    val sqlDelight = sqlDelight(hikari)

    val userPersistence = userPersistence(sqlDelight.usersQueries, sqlDelight.userThemesQueries)
    val subjectPersistence = subjectPersistence(sqlDelight.subjectsQueries)
    val quizPersistence = quizPersistence(sqlDelight.quizzesQueries)
    val questionPersistence =
        questionPersistence(
            sqlDelight.questionsQueries,
            sqlDelight.questionOptionsQueries,
            sqlDelight.questionAttemptsQueries
        )
    val questionOptionsPersistence = questionOptionsPersistence(sqlDelight.questionOptionsQueries)
    val questionAttemptsPersistence = questionAttemptPersistence(sqlDelight.questionAttemptsQueries)
    val languagePersistence = languagePersistence(sqlDelight.languagesQueries)

    val subjectService = subjectService(subjectPersistence)
    val jwtService = jwtService(env.auth, userPersistence)
    val userService = userService(userPersistence, jwtService)
    val gptAIService = gptAIService(openAI.token)
    val languageService = languageService(languagePersistence)

    val quizService =
        quizService(
            userPersistence,
            subjectPersistence,
            quizPersistence,
            questionPersistence,
            questionOptionsPersistence,
            questionAttemptsPersistence,
            gptAIService,
            coroutineScope
        )

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
        quizService = quizService,
        subjectService = subjectService,
        languageService = languageService
    )
}

suspend fun Dependencies.startEventListeners() = quizService.listenEvent()
