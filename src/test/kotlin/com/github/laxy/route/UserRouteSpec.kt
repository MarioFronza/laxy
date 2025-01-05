package com.github.laxy.route

import com.github.laxy.withServer
import io.kotest.core.spec.style.StringSpec
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.post
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
            val response = post(UserResource()) {
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
})