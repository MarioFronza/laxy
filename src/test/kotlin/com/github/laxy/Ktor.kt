@file:Suppress("MatchingDeclarationName")

package com.github.laxy

import com.github.laxy.env.Dependencies
import com.github.laxy.env.kotlinXSerializersModule
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.resources.Resources
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.TestApplication
import kotlinx.serialization.json.Json

suspend fun withServer(test: suspend HttpClient.(dep: Dependencies) -> Unit) {
    val dependencies = KotestProject.dependencies.get()
    testApplication {
        application { app(dependencies) }
        createClient {
                expectSuccess = false
                install(ContentNegotiation) {
                    json(Json { serializersModule = kotlinXSerializersModule })
                }
                install(Resources) { serializersModule = kotlinXSerializersModule }
            }
            .use { client -> test(client, dependencies) }
    }
}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
private suspend fun testApplication(block: suspend ApplicationTestBuilder.() -> Unit) {
    val builder = ApplicationTestBuilder().apply { block() }
    val testApplication = TestApplication(builder)
    testApplication.engine.start()
    testApplication.stop()
}
