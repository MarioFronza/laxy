package com.github.laxy.env

import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.maxAgeDuration
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.resources.Resources
import kotlin.time.Duration.Companion.days
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

val kotlinXSerializerModule = SerializersModule {}

fun Application.configure() {
    install(DefaultHeaders)
    install(Resources) { serializersModule = kotlinXSerializerModule }
    install(ContentNegotiation) {
        json(
            Json {
                serializersModule = kotlinXSerializerModule
                isLenient = true
                ignoreUnknownKeys = true
            }
        )
    }
    install(CORS) {
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowNonSimpleContentTypes = true
        maxAgeDuration = 3.days
    }
}
