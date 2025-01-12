package com.github.laxy.route

import com.github.laxy.service.RegisterUser
import com.github.laxy.withServer
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.post
import io.ktor.client.plugins.resources.put
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

class UserRouteSpec : StringSpec({
    val validUsername = "username"
    val validEmail = "valid@domain.com"
    val validPassword = "123456789"

    "given a valid input, when calls [POST] /users, should return Created status code" {
        withServer {
            val response =
                post(UsersResource()) {
                    contentType(ContentType.Application.Json)
                    setBody(UserWrapper(NewUser(validUsername, validEmail, validPassword)))
                }

            assert(response.status == HttpStatusCode.Created)
            with(response.body<UserWrapper<User>>().user) {
                assert(username == validUsername)
                assert(email == validEmail)
            }
        }
    }

    "given a valid input, when calls [POST] /users/login, should return OK status code" {
        withServer { dependencies ->
            dependencies.userService
                .register(RegisterUser(validUsername, validEmail, validPassword))
                .shouldBeRight()

            val response = post(UsersResource.Login()) {
                contentType(ContentType.Application.Json)
                setBody(UserWrapper(LoginUser(validEmail, validPassword)))
            }

            assert(response.status == HttpStatusCode.OK)
            with(response.body<UserWrapper<User>>().user) {
                assert(username == validUsername)
                assert(email == validEmail)
            }
        }
    }

    "given a valid input, when calls [GET] /user, should return OK status code" {
        withServer { dependencies ->
            val expectedToken = dependencies.userService
                .register(RegisterUser(validUsername, validEmail, validPassword))
                .shouldBeRight()

            val response = get(UserResource()) {
                bearerAuth(expectedToken.value)
            }

            assert(response.status == HttpStatusCode.OK)

            with(response.body<UserWrapper<User>>().user) {
                assert(username == validUsername)
                assert(email == validEmail)
                assert(token == expectedToken.value)
            }
        }
    }

    "given a valid input, when calls [PUT] /user, should return OK status code" {
        withServer { dependencies ->
            val expectedToken = dependencies.userService
                .register(RegisterUser(validUsername, validEmail, validPassword))
                .shouldBeRight()
            val newUsername = "newUsername"

            val response = put(UserResource()) {
                bearerAuth(expectedToken.value)
                contentType(ContentType.Application.Json)
                setBody(UserWrapper(UpdateUser(username = newUsername)))
            }

            assert(response.status == HttpStatusCode.OK)
            with(response.body<UserWrapper<User>>().user) {
                assert(username == newUsername)
                assert(email == validEmail)
                assert(token == expectedToken.value)
            }
        }
    }

    "given a invalid email, when calls [PUT] /user, should return UnprocessableEntity status code" {
        withServer { dependencies ->
            val expectedToken = dependencies.userService
                .register(RegisterUser(validUsername, validEmail, validPassword))
                .shouldBeRight()
            val invalidEmail = "invalidEmail"

            val response = put(UserResource()) {
                bearerAuth(expectedToken.value)
                contentType(ContentType.Application.Json)
                setBody(UserWrapper(UpdateUser(email = invalidEmail)))
            }

            assert(response.status == HttpStatusCode.UnprocessableEntity)
            assert(
                response.body<GenericErrorModel>().errors.body ==
                        listOf("email: 'invalidEmail' is invalid email")
            )
        }
    }
})