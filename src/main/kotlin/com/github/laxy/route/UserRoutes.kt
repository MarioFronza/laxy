package com.github.laxy.route

import com.github.laxy.domain.auth.JwtService
import com.github.laxy.domain.user.RegisterUser
import com.github.laxy.domain.user.UserService
import com.github.laxy.shared.Failure
import com.github.laxy.shared.IllegalStateError
import com.github.laxy.shared.InteractionResult
import com.github.laxy.shared.Success
import com.github.laxy.shared.interaction
import io.ktor.http.HttpStatusCode
import io.ktor.resources.Resource
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.util.pipeline.PipelineContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.Serializable

@Serializable
data class UserWrapper<T : Any>(val user: T)

@Serializable
data class NewUser(
    val username: String,
    val email: String,
    val password: String
)

@Serializable
data class UpdateUser(
    val email: String? = null,
    val username: String? = null,
    val password: String? = null,
    val bio: String? = null,
    val image: String? = null
)

@Serializable
data class User(
    val email: String,
    val token: String,
    val username: String,
    val bio: String,
    val image: String
)

@Serializable
data class LoginUser(
    val email: String,
    val password: String
)

@Resource("/users")
data class UsersResource(val parent: RootResource = RootResource) {
    @Resource("/login")
    data class Login(val parent: UsersResource = UsersResource())
}

@Resource("/user")
data class UserResource(val parent: RootResource = RootResource)

fun Route.userRoutes(
    userService: UserService,
    jwtService: JwtService
) {
    post<UserResource> {
        interaction {
            val (username, email, password) = receiveCatching<UserWrapper<NewUser>>().bind().user
            val token = userService.register(RegisterUser(username, email, password)).bind().serial
            val wrapper = UserWrapper(User(email, token.toString(), username, "", ""))
            Success(wrapper)
        }.respond(HttpStatusCode.OK)
    }
}

@OptIn(ExperimentalSerializationApi::class)
private suspend inline fun <reified D : Any> PipelineContext<Unit, ApplicationCall>.receiveCatching(): InteractionResult<D> {
    return try {
        Success(call.receive())
    } catch (e: MissingFieldException) {
        val message = e.message ?: "Incorrect Json"
        Failure(IllegalStateError.illegalState(message))
    }
}