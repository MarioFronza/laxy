package com.github.laxy

import com.github.laxy.env.DependencyRegistry
import com.github.laxy.env.Env
import com.github.laxy.env.configure
import com.github.laxy.env.dependencies
import com.github.laxy.route.health
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    val env = Env()
    val dependencies = dependencies(env)
    embeddedServer(
            port = env.http.port,
        factory = Netty,
        host = env.http.host,
        ) {
            module(dependencies)
        }
        .start(wait = true)
}

fun Application.module(dependencyRegistry: DependencyRegistry) {
    configure()
    health(dependencyRegistry.healthCheck)
}
