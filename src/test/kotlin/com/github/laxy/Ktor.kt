@file:Suppress("MatchingDeclarationName")

package com.github.laxy

import com.github.laxy.env.DependencyRegistry
import com.github.laxy.env.kotlinXSerializersModule
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.resources.Resources
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json


suspend fun withServer(test: suspend HttpClient.(dep: DependencyRegistry) -> Unit): Unit {
    val dependencies = KotestProject.dependencies.get()
    testApplication {
        application { app(dependencies) }
        createClient {
            expectSuccess = false
            install(ContentNegotiation) { json(Json { serializersModule = kotlinXSerializersModule }) }
            install(Resources) { serializersModule = kotlinXSerializersModule }
        }
            .use { client -> test(client, dependencies) }
    }
}
