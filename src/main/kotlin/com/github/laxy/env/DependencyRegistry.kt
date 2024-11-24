package com.github.laxy.env

import com.github.laxy.domain.ai.GptAIService
import com.github.laxy.domain.ai.gptAIService
import com.github.laxy.domain.user.UserService
import com.github.laxy.domain.user.userService
import com.github.laxy.persistence.userPersistence
import com.sksamuel.cohort.HealthCheckRegistry
import com.sksamuel.cohort.hikari.HikariConnectionsHealthCheck
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers

class DependencyRegistry(
    val healthCheck: HealthCheckRegistry,
    val userService: UserService,
    val gptAIService: GptAIService
)

fun dependencies(env: Env): DependencyRegistry {
    val hikari = hikari(env.dataSource)
    val openAI = env.openAI
    val sqlDelight = sqlDelight(hikari)
    val userPersistence = userPersistence(sqlDelight.usersQueries)
    val userService = userService(userPersistence)
    val gptAIService = gptAIService(openAI.token)
    val checks =
        HealthCheckRegistry(Dispatchers.Default) {
            register(
                check = HikariConnectionsHealthCheck(hikari, 1),
                initialDelay = 1.seconds,
                checkInterval = 5.seconds
            )
        }
    return DependencyRegistry(
        healthCheck = checks,
        userService = userService,
        gptAIService = gptAIService
    )
}
