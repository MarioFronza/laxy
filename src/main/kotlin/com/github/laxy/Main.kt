package com.github.laxy

import arrow.continuations.SuspendApp
import arrow.continuations.ktor.server
import arrow.fx.coroutines.resourceScope
import com.github.laxy.env.DependencyRegistry
import com.github.laxy.env.Env
import com.github.laxy.env.configure
import com.github.laxy.env.dependencies
import com.github.laxy.route.health
import io.ktor.server.application.Application
import io.ktor.server.netty.Netty
import kotlinx.coroutines.awaitCancellation

fun main() = SuspendApp {
    val env = Env()
    resourceScope {
        val dependencies = dependencies(env)
        server(
            port = env.http.port,
            factory = Netty,
            host = env.http.host,
        ) {
            app(dependencies)
        }
        awaitCancellation()
    }
}

fun Application.app(dependencyRegistry: DependencyRegistry) {
    configure()
    health(dependencyRegistry.healthCheck)
}
