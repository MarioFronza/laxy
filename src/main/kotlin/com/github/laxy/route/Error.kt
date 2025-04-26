package com.github.laxy.route

import arrow.core.Either
import com.github.laxy.DomainError
import com.github.laxy.EmailAlreadyExists
import com.github.laxy.EmptyUpdate
import com.github.laxy.IncorrectInput
import com.github.laxy.IncorrectJson
import com.github.laxy.InvalidIntegrationResponse
import com.github.laxy.JwtGeneration
import com.github.laxy.JwtInvalid
import com.github.laxy.MissingParameter
import com.github.laxy.PasswordNotMatched
import com.github.laxy.QuestionCreationError
import com.github.laxy.QuestionOptionCreationError
import com.github.laxy.QuizAttemptError
import com.github.laxy.QuizCreationError
import com.github.laxy.SubjectNotFound
import com.github.laxy.UserNotFound
import com.github.laxy.UserThemeNotFound
import com.github.laxy.UsernameAlreadyExists
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.util.pipeline.PipelineContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@Serializable data class GenericErrorModel(val errors: GenericErrorModelErrors)

@Serializable data class GenericErrorModelErrors(val body: List<String>)

suspend inline fun <reified A : Any> Either<DomainError, A>.respond(
    context: PipelineContext<Unit, ApplicationCall>,
    status: HttpStatusCode
) {
    when (this) {
        is Either.Left -> context.respond(value)
        is Either.Right -> context.call.respond(status, value)
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Suppress("CyclomaticComplexMethod")
suspend fun PipelineContext<Unit, ApplicationCall>.respond(error: DomainError): Unit =
    when (error) {
        PasswordNotMatched -> call.respond(HttpStatusCode.Unauthorized)
        is IncorrectInput ->
            unprocessable(
                error.errors.map { field -> "${field.field}: ${field.errors.joinToString()}" }
            )
        is IncorrectJson ->
            unprocessable("Json is missing fields: ${error.exception.missingFields.joinToString()}")
        is EmptyUpdate -> unprocessable(error.description)
        is EmailAlreadyExists -> unprocessable("${error.email} is already registered")
        is JwtGeneration -> unprocessable(error.description)
        is UserNotFound -> unprocessable("User with ${error.property} not found")
        is UsernameAlreadyExists -> unprocessable("Username ${error.username} already exists")
        is JwtInvalid -> unprocessable(error.description)
        is MissingParameter -> unprocessable("Missing ${error.name} parameter in request")
        is InvalidIntegrationResponse ->
            unprocessable("Invalid GPT api content response: ${error.content}")
        is SubjectNotFound -> unprocessable("Subject with ${error.property} not found")
        is UserThemeNotFound -> unprocessable("Theme with ${error.property} not found")
        is QuizCreationError -> unprocessable("Creation quiz unexpected error")
        is QuestionCreationError -> unprocessable("Creation quiz question unexpected error")
        is QuestionOptionCreationError ->
            unprocessable("Creation quiz question option unexpected error")
        is QuizAttemptError -> unprocessable("Quiz attempt unexpected error")
    }

private suspend inline fun PipelineContext<Unit, ApplicationCall>.unprocessable(
    error: String
): Unit =
    call.respond(
        HttpStatusCode.UnprocessableEntity,
        GenericErrorModel(GenericErrorModelErrors(listOf(error)))
    )

private suspend inline fun PipelineContext<Unit, ApplicationCall>.unprocessable(
    errors: List<String>
): Unit =
    call.respond(
        HttpStatusCode.UnprocessableEntity,
        GenericErrorModel(GenericErrorModelErrors(errors))
    )
