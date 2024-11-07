package com.github.laxy.route

import com.github.laxy.domain.user.UserService
import io.ktor.server.routing.Route

data class UsersResource(val parent: RootResource)

fun Route.userRoutes(userService: UserService) {}
