package com.github.laxy.route

import com.github.laxy.env.Dependencies
import io.ktor.resources.Resource
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.routes(deps: Dependencies) = routing {
    userRoutes(deps.userService, deps.jwtService)
}

@Resource("/api") data object RootResource
