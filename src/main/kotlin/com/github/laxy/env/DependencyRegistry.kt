package com.github.laxy.env

import com.github.laxy.domain.user.UserService
import com.github.laxy.domain.user.userService
import com.github.laxy.persistence.userPersistence
import com.sksamuel.cohort.HealthCheckRegistry
import com.sksamuel.cohort.hikari.HikariConnectionsHealthCheck
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers

class DependencyRegistry(val healthCheck: HealthCheckRegistry, val userService: UserService)

fun dependencies(env: Env): DependencyRegistry {
    val hikari = hikari(env.dataSource)
    val sqlDelight = sqlDelight(hikari)
    val userPersistence = userPersistence(sqlDelight.usersQueries)
    val userService = userService(userPersistence)
    val checks =
        HealthCheckRegistry(Dispatchers.Default) {
            register(
                check = HikariConnectionsHealthCheck(hikari, 1),
                initialDelay = 1.seconds,
                checkInterval = 5.seconds
            )
        }
    return DependencyRegistry(healthCheck = checks, userService)
}
