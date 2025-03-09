package com.github.laxy.route

import arrow.core.raise.either
import com.github.laxy.auth.jwtAuth
import com.github.laxy.persistence.LanguageId
import com.github.laxy.service.JwtService
import com.github.laxy.service.LanguageService
import com.github.laxy.service.SubjectService
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.resources.Resource
import io.ktor.server.resources.get
import io.ktor.server.routing.Route
import kotlinx.serialization.Serializable

@Serializable data class LanguagesWrapper<T : Any>(val languages: T)

@Serializable data class Language(val id: Long, val name: String, val code: String)

@Serializable data class LanguageSubject(val id: Long, val name: String, val description: String)

@Resource("/languages")
data class LanguagesResource(val parent: RootResource = RootResource) {
    @Resource("/{id}/subjects")
    data class LanguageSubjectsResource(
        val id: Long,
        val parent: LanguagesResource = LanguagesResource()
    )
}

fun Route.languageRoutes(
    languageService: LanguageService,
    subjectService: SubjectService,
    jwtService: JwtService
) {
    get<LanguagesResource> {
        jwtAuth(jwtService) {
            either {
                    val languages = languageService.getAllLanguages().bind()
                    LanguagesWrapper(
                        languages.map {
                            Language(id = it.id.serial, name = it.name, code = it.code)
                        }
                    )
                }
                .respond(this, OK)
        }
    }

    get<LanguagesResource.LanguageSubjectsResource> { resource ->
        jwtAuth(jwtService) {
            either {
                    val subjects =
                        subjectService.getAllSubjectsByLanguage(LanguageId(resource.id)).bind()
                    SubjectsWrapper(
                        subjects.map {
                            LanguageSubject(
                                id = it.id.serial,
                                name = it.name,
                                description = it.description
                            )
                        }
                    )
                }
                .respond(this, OK)
        }
    }
}
