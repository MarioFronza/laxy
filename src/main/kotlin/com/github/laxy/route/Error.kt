@file:OptIn(ExperimentalSerializationApi::class)

package com.github.laxy.route

import arrow.core.Either
import com.github.laxy.DomainError
import com.github.laxy.EmailAlreadyExists
import com.github.laxy.EmptyUpdate
import com.github.laxy.IncorrectInput
import com.github.laxy.IncorrectJson
import com.github.laxy.JwtGeneration
import com.github.laxy.JwtInvalid
import com.github.laxy.MissingParameter
import com.github.laxy.PasswordNotMatched
import com.github.laxy.UserNotFound
import com.github.laxy.UsernameAlreadyExists
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.util.pipeline.PipelineContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@Serializable
data class GenericErrorModel(val errors: GenericErrorModelErrors)

@Serializable
data class GenericErrorModelErrors(val body: List<String>)

context(PipelineContext<Unit, ApplicationCall>)
suspend inline fun <reified A : Any> Either<DomainError, A>.respond(status: HttpStatusCode) {
    when (this) {
        is Either.Left -> respond(value)
        is Either.Right -> call.respond(status, value)
    }
}

suspend fun PipelineContext<Unit, ApplicationCall>.respond(error: DomainError) {
    when (error) {
        PasswordNotMatched -> call.respond(HttpStatusCode.Unauthorized)
        is JwtGeneration -> unprocessable(error.description)
        is JwtInvalid -> unprocessable(error.description)
        is EmailAlreadyExists -> unprocessable("${error.email} is already registered")
        is UserNotFound -> unprocessable("User with ${error.property} not found")
        is UsernameAlreadyExists -> unprocessable("User with ${error.username} not found")
        is EmptyUpdate -> unprocessable(error.description)
        is IncorrectInput -> unprocessable(error.errors.map { field -> "${field.field}: ${field.errors.joinToString()}" })
        is IncorrectJson -> unprocessable("Json is missing fields: ${error.exception.missingFields.joinToString()}")
        is MissingParameter -> unprocessable("Missing ${error.name} parameter in request")
    }
}

private suspend inline fun PipelineContext<Unit, ApplicationCall>.unprocessable(error: String) = call.respond(
    HttpStatusCode.UnprocessableEntity, GenericErrorModel(GenericErrorModelErrors(listOf(error)))
)

private suspend inline fun PipelineContext<Unit, ApplicationCall>.unprocessable(
    errors: List<String>
) = call.respond(
    HttpStatusCode.UnprocessableEntity, GenericErrorModel(GenericErrorModelErrors(errors))
)
