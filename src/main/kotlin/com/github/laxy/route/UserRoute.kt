package com.github.laxy.route

import arrow.core.Either
import arrow.core.raise.either
import com.github.laxy.service.JwtService
import com.github.laxy.auth.jwtAuth
import com.github.laxy.service.Login
import com.github.laxy.service.RegisterUser
import com.github.laxy.service.UpdateUser
import com.github.laxy.service.UserService
import com.github.laxy.IncorrectJson
import io.ktor.http.HttpStatusCode
import io.ktor.resources.Resource
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.resources.get
import io.ktor.server.resources.put
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.util.pipeline.PipelineContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.Serializable

@Serializable
data class UserWrapper<T : Any>(val user: T)

@Serializable
data class NewUser(val username: String, val email: String, val password: String)

@Serializable
data class UpdateUserRequest(
    val email: String? = null,
    val username: String? = null,
    val password: String? = null
)

@Serializable
data class User(val email: String, val token: String, val username: String)

@Serializable
data class LoginUser(val email: String, val password: String)

@Resource("/users")
data class UsersResource(val parent: RootResource = RootResource) {
    @Resource("/login")
    data class Login(val parent: UsersResource = UsersResource())
}

@Resource("/user")
data class UserResource(val parent: RootResource = RootResource)

fun Route.userRoutes(userService: UserService, jwtService: JwtService) {
    post<UsersResource> {
        either {
            val (username, email, password) = receiveCatching<UserWrapper<NewUser>>().bind().user
            val token = userService.register(RegisterUser(username, email, password)).bind().value
            UserWrapper(User(email, token, username))
        }.respond(HttpStatusCode.OK)
    }

    post<UsersResource.Login> {
        either {
            val (email, password) = receiveCatching<UserWrapper<LoginUser>>().bind().user
            val (token, info) = userService.login(Login(email, password)).bind()
            UserWrapper(User(email, token.value, info.username))
        }.respond(HttpStatusCode.OK)
    }

    get<UserResource> {
        jwtAuth(jwtService) { (token, userId) ->
            either {
                val info = userService.getUser(userId).bind()
                UserWrapper(User(info.email, token.value, info.username))
            }.respond(HttpStatusCode.OK)
        }
    }

    put<UserResource> {
        jwtAuth(jwtService) { (token, userId) ->
            either {
                val (email, username, password) = receiveCatching<UserWrapper<UpdateUserRequest>>().bind().user
                val info = userService.update(UpdateUser(userId, username, email, password)).bind()
                UserWrapper(User(info.email, token.value, info.username))
            }.respond(HttpStatusCode.OK)
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
private suspend inline fun <reified A : Any> PipelineContext<Unit, ApplicationCall>.receiveCatching(): Either<IncorrectJson, A> =
    Either.catchOrThrow<MissingFieldException, A> { call.receive() }.mapLeft { IncorrectJson(it) }
