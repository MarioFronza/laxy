package com.github.laxy.env

import com.github.laxy.persistence.UserPersistence
import com.github.laxy.persistence.userPersistence
import com.sksamuel.cohort.HealthCheckRegistry
import com.sksamuel.cohort.hikari.HikariConnectionsHealthCheck
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers

class Dependencies(val healthCheck: HealthCheckRegistry, userPersistence: UserPersistence)

fun dependencies(env: Env): Dependencies {
    val hikari = hikari(env.dataSource)
    val sqlDelight = sqlDelight(hikari)
    val userPersistence = userPersistence(sqlDelight.usersQueries)
    val checks =
        HealthCheckRegistry(Dispatchers.Default) {
            register(
                check = HikariConnectionsHealthCheck(hikari, 1),
                initialDelay = 1.seconds,
                checkInterval = 5.seconds
            )
        }
    return Dependencies(healthCheck = checks, userPersistence = userPersistence)
}
