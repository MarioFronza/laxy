package com.github.laxy.env

import arrow.fx.coroutines.continuations.ResourceScope
import com.github.laxy.persistence.userPersistence
import com.github.laxy.service.GptAIService
import com.github.laxy.service.JwtService
import com.github.laxy.service.UserService
import com.github.laxy.service.gptAIService
import com.github.laxy.service.jwtService
import com.github.laxy.service.userService
import com.sksamuel.cohort.HealthCheckRegistry
import com.sksamuel.cohort.hikari.HikariConnectionsHealthCheck
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers

class DependencyRegistry(
    val healthCheck: HealthCheckRegistry,
    val userService: UserService,
    val gptAIService: GptAIService,
    val jwtService: JwtService
)

suspend fun ResourceScope.dependencies(env: Env): DependencyRegistry {
    val hikari = hikari(env.dataSource)
    val openAI = env.openAI
    val sqlDelight = sqlDelight(hikari)
    val userPersistence = userPersistence(sqlDelight.usersQueries)
    val jwtService = jwtService(env.auth, userPersistence)
    val userService = userService(userPersistence, jwtService)
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
        gptAIService = gptAIService,
        jwtService = jwtService
    )
}
