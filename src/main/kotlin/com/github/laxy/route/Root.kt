package com.github.laxy.route

import arrow.core.Either
import com.github.laxy.IncorrectJson
import com.github.laxy.env.Dependencies
import io.ktor.resources.Resource
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.routing.routing
import io.ktor.util.pipeline.PipelineContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException

fun Application.routes(deps: Dependencies) = routing {
    userRoutes(deps.userService, deps.jwtService)
    quizRoutes(deps.quizService, deps.jwtService)
    subjectRoutes(deps.subjectService, deps.jwtService)
    languageRoutes(deps.languageService, deps.subjectService, deps.jwtService)
}

@Resource("/api") data object RootResource

@OptIn(ExperimentalSerializationApi::class)
suspend inline fun <reified A : Any> PipelineContext<Unit, ApplicationCall>.receiveCatching():
    Either<IncorrectJson, A> =
    Either.catchOrThrow<MissingFieldException, A> { call.receive() }.mapLeft { IncorrectJson(it) }
