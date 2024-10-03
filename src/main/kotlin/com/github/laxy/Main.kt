package com.github.laxy

import com.github.laxy.env.Dependencies
import com.github.laxy.env.Env
import com.github.laxy.env.dependencies
import com.github.laxy.routes.health
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    val env = Env()
    val dependencies = dependencies(env)
    embeddedServer(
            factory = Netty,
            port = env.http.port,
            host = env.http.host,
        ) {
            module(dependencies)
        }
        .start(wait = true)
}

fun Application.module(dependencies: Dependencies) {
    health(dependencies.healthCheck)
}
