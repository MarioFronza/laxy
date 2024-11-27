package com.github.laxy.route

import com.github.laxy.shared.ApplicationError
import com.github.laxy.shared.IllegalStateError
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.util.pipeline.PipelineContext
import kotlinx.serialization.Serializable

@Serializable
data class GenericErrorModel(val errors: GenericErrorModelErrors)

@Serializable
data class GenericErrorModelErrors(val body: List<String>)

suspend fun PipelineContext<Unit, ApplicationCall>.respond(error: ApplicationError) {
    when (error) {
        is IllegalStateError -> unprocessable(error.errors)
    }
}

private suspend inline fun PipelineContext<Unit, ApplicationCall>.unprocessable(
    error: String
) = call.respond(
    HttpStatusCode.UnprocessableEntity,
    GenericErrorModel(GenericErrorModelErrors(listOf(error)))
)

private suspend inline fun PipelineContext<Unit, ApplicationCall>.unprocessable(
    errors: List<String>
) = call.respond(
    HttpStatusCode.UnprocessableEntity,
    GenericErrorModel(GenericErrorModelErrors(errors))
)