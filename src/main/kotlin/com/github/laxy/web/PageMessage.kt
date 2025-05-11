@file:OptIn(ExperimentalSerializationApi::class)

package com.github.laxy.web

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
import com.github.laxy.QuizSelectionError
import com.github.laxy.SubjectNotFound
import com.github.laxy.UserNotFound
import com.github.laxy.UserThemeNotFound
import com.github.laxy.UsernameAlreadyExists
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import io.ktor.server.thymeleaf.ThymeleafContent
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@Serializable data class PageMessage(val type: String, val message: String)

suspend fun ApplicationCall.respondTemplate(
    name: String,
    data: Map<String, Any> = emptyMap(),
    message: PageMessage? = null
) {
    val mutableData = data.toMutableMap()
    if (message != null) {
        mutableData["message"] = message
    }
    return this.respond(ThymeleafContent(name, mutableData))
}

@Suppress("CyclomaticComplexMethod")
fun DomainError.toPageMessage(): PageMessage =
    when (this) {
        is InvalidIntegrationResponse ->
            PageMessage("error", "There was an issue communicating with the external service.")
        is JwtGeneration -> PageMessage("error", "Failed to generate authentication token.")
        is JwtInvalid ->
            PageMessage("error", "Your session is invalid or expired. Please log in again.")
        is QuestionCreationError ->
            PageMessage("error", "An unexpected error occurred while creating a question.")
        is QuestionOptionCreationError ->
            PageMessage("error", "An error occurred while creating question options.")
        is QuizAttemptError ->
            PageMessage("error", "We couldn't process your quiz answers. Try again.")
        is QuizCreationError -> PageMessage("error", "An error occurred while creating your quiz.")
        is QuizSelectionError ->
            PageMessage("error", "Unable to load your quiz. Please try again later.")
        is SubjectNotFound -> PageMessage("error", "The selected subject could not be found.")
        is EmailAlreadyExists ->
            PageMessage("error", "The email ${this.email} is already registered.")
        is PasswordNotMatched -> PageMessage("error", "Passwords do not match.")
        is UserNotFound -> PageMessage("error", "User not found by ${this.property}.")
        is UserThemeNotFound ->
            PageMessage("error", "Could not find your theme based on ${this.property}.")
        is UsernameAlreadyExists ->
            PageMessage("error", "The username '${this.username}' is already taken.")
        is EmptyUpdate -> PageMessage("error", this.description)
        is IncorrectInput ->
            PageMessage(
                "error",
                "Fix the following issues:\n" +
                    this.errors.joinToString("\n") { "${it.field}: ${it.errors.joinToString()}" }
            )
        is IncorrectJson ->
            PageMessage(
                "error",
                "Missing fields in JSON: ${this.exception.missingFields.joinToString()}"
            )
        is MissingParameter -> PageMessage("error", "The '${this.name}' parameter is required.")
    }

fun ApplicationCall.consumeFlashMessage(): PageMessage? {
    val flash = sessions.get<PageMessage>()
    if (flash != null) sessions.clear<PageMessage>()
    return flash
}

suspend fun ApplicationCall.redirectWithFlash(path: String, message: PageMessage) {
    sessions.set(message)
    respondRedirect(path)
}

suspend fun ApplicationCall.successRedirect(path: String, msg: String) =
    redirectWithFlash(path, PageMessage("success", msg))

suspend fun ApplicationCall.errorRedirect(path: String, msg: String) =
    redirectWithFlash(path, PageMessage("error", msg))

suspend fun ApplicationCall.respondTemplateWithFlash(
    name: String,
    data: Map<String, Any> = emptyMap()
) {
    val flash = consumeFlashMessage()
    respond(respondTemplate(name, data, message = flash))
}
