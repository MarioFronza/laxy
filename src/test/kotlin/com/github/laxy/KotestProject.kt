package com.github.laxy

import arrow.fx.coroutines.continuations.resource
import com.github.laxy.env.Env
import com.github.laxy.env.dependencies
import com.github.laxy.env.hikari
import io.kotest.assertions.arrow.fx.coroutines.ProjectResource
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.extensions.Extension
import io.kotest.core.listeners.TestListener
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.extensions.testcontainers.StartablePerProjectListener
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait

private class PostgreSQL : PostgreSQLContainer<PostgreSQL>("postgres:latest") {
    init {
        waitingFor(Wait.forListeningPorts())
    }
}

object KotestProject : AbstractProjectConfig() {
    private val postgres = PostgreSQL()

    private val dataSource: Env.DataSource by lazy {
        Env.DataSource(
            postgres.jdbcUrl,
            postgres.username,
            postgres.password,
            postgres.driverClassName
        )
    }

    private val env: Env by lazy { Env().copy(dataSource = dataSource) }

    val dependencies = ProjectResource(resource { dependencies(env) })
    val hikari = ProjectResource(resource { hikari(env.dataSource) })

    override val globalAssertSoftly: Boolean = true

    override fun extensions(): List<Extension> =
        listOf(StartablePerProjectListener(postgres), hikari, dependencies, resetDatabaseListener)

    private val resetDatabaseListener =
        object : TestListener {
            override suspend fun afterTest(testCase: TestCase, result: TestResult) {
                super.afterTest(testCase, result)
                hikari.get().connection.use { conn ->
                    conn.prepareStatement("TRUNCATE users CASCADE").executeLargeUpdate()
                }
            }
        }
}
