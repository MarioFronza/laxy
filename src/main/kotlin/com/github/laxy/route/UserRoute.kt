package com.github.laxy.route

import arrow.core.raise.either
import com.github.laxy.auth.jwtAuth
import com.github.laxy.service.CreateTheme
import com.github.laxy.service.JwtService
import com.github.laxy.service.Login
import com.github.laxy.service.RegisterUser
import com.github.laxy.service.Update
import com.github.laxy.service.UserService
import io.ktor.http.HttpStatusCode
import io.ktor.resources.Resource
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.routing.Route
import kotlinx.serialization.Serializable

@Serializable data class UserWrapper<T : Any>(val user: T)

@Serializable data class UserThemeWrapper<T : Any>(val theme: T)

@Serializable data class NewUser(val username: String, val email: String, val password: String)

@Serializable data class NewTheme(val description: String)

@Serializable
data class UpdateUser(
    val email: String? = null,
    val username: String? = null,
    val password: String? = null
)

@Serializable data class User(val email: String, val token: String, val username: String)

@Serializable data class LoginUser(val email: String, val password: String)

@Serializable data class UserTheme(val description: String)

@Resource("/users")
data class UsersResource(val parent: RootResource = RootResource) {
    @Resource("/login") data class Login(val parent: UsersResource = UsersResource())

    @Resource("/theme") data class Theme(val parent: UsersResource = UsersResource())
}

@Resource("/user") data class UserResource(val parent: RootResource = RootResource)

fun Route.userRoutes(
    userService: UserService,
    jwtService: JwtService,
) {
    post<UsersResource> {
        either {
                val (username, email, password) =
                    receiveCatching<UserWrapper<NewUser>>().bind().user
                val token =
                    userService.register(RegisterUser(username, email, password)).bind().value
                UserWrapper(User(email, token, username))
            }
            .respond(this, HttpStatusCode.Created)
    }
    post<UsersResource.Login> {
        either {
                val (email, password) = receiveCatching<UserWrapper<LoginUser>>().bind().user
                val (token, info) = userService.login(Login(email, password)).bind()
                UserWrapper(User(email, token.value, info.username))
            }
            .respond(this, HttpStatusCode.OK)
    }
    post<UsersResource.Theme> {
        jwtAuth(jwtService) { (_, userId) ->
            either {
                    val (description) = receiveCatching<UserThemeWrapper<NewTheme>>().bind().theme
                    val theme = userService.createTheme(CreateTheme(userId, description)).bind()
                    UserWrapper(UserTheme(theme.description))
                }
                .respond(this, HttpStatusCode.Created)
        }
    }
    get<UserResource> {
        jwtAuth(jwtService) { (token, userId) ->
            either {
                    val info = userService.getUser(userId).bind()
                    UserWrapper(User(info.email, token.value, info.username))
                }
                .respond(this, HttpStatusCode.OK)
        }
    }
    put<UserResource> {
        jwtAuth(jwtService) { (token, userId) ->
            either {
                    val (email, username, password) =
                        receiveCatching<UserWrapper<UpdateUser>>().bind().user
                    val info = userService.update(Update(userId, username, email, password)).bind()
                    UserWrapper(User(info.email, token.value, info.username))
                }
                .respond(this, HttpStatusCode.OK)
        }
    }
}
