package com.github.laxy

import com.github.laxy.env.Env
import com.github.laxy.env.dependencies
import com.github.laxy.routes.health
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    embeddedServer(Netty, port = 9292, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val env = Env()
    val dependencies = dependencies(env)
    health(dependencies.healthCheck)
}
