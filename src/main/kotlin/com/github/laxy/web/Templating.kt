package com.github.laxy.web

import com.github.laxy.service.QuizService
import com.github.laxy.service.SubjectService
import com.github.laxy.service.UserService
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.http.content.staticResources
import io.ktor.server.routing.routing
import io.ktor.server.thymeleaf.Thymeleaf
import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver

fun Application.configureTemplating(
    userService: UserService,
    quizService: QuizService,
    subjectService: SubjectService
) {
    install(Thymeleaf) {
        setTemplateResolver(
            ClassLoaderTemplateResolver().apply {
                prefix = "templates/"
                suffix = ".html"
                characterEncoding = "utf-8"
                addDialect(LayoutDialect())
            }
        )
    }
    routing {
        staticResources("/static", "static")
        authRoutes(userService)
        quizRoutes(quizService, subjectService, userService)
    }
}
