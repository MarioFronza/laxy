package com.github.laxy.web

import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.http.content.staticResources
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.thymeleaf.Thymeleaf
import io.ktor.server.thymeleaf.ThymeleafContent
import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import org.thymeleaf.templateresolver.FileTemplateResolver

fun Application.configureTemplating() {
    install(Thymeleaf) {
        setTemplateResolver(
            (if (developmentMode) {
                FileTemplateResolver().apply {
                    cacheManager = null
                    prefix = "src/main/resources/templates/"
                }
            } else {
                ClassLoaderTemplateResolver().apply {
                    prefix = "templates/"
                }
            }).apply {
                suffix = ".html"
                characterEncoding = "utf-8"
                addDialect(LayoutDialect())
            })
    }
    routing {
        staticResources("/static", "static")

        get("/") {
            call.respond(ThymeleafContent("index", emptyMap()))
        }

        get("/signin") {
            call.respond(ThymeleafContent("signin", emptyMap()))
        }

        post("/signin") {
            val params = call.receiveParameters()
            val email = params["email"]
            val password = params["password"]

            if (email == "user@example.com" && password == "password") {
                call.respondRedirect("/")
            } else {
                call.respond(ThymeleafContent("signin", mapOf("error" to "Invalid credentials")))
            }
        }

        get("/signup") {
            call.respond(ThymeleafContent("signup", emptyMap()))
        }

        post("/signup") {
            val params = call.receiveParameters()
            val username = params["username"]
            val email = params["email"]
            val password = params["password"]

            // TODO: Save user in database
            call.respondRedirect("/signin")
        }
    }
}
