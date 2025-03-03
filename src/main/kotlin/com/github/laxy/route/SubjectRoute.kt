package com.github.laxy.route

import arrow.core.raise.either
import com.github.laxy.auth.jwtAuth
import com.github.laxy.persistence.SubjectId
import com.github.laxy.service.JwtService
import com.github.laxy.service.SubjectService
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.resources.Resource
import io.ktor.server.resources.get
import io.ktor.server.routing.Route
import kotlinx.serialization.Serializable


@Serializable
data class SubjectWrapper<T : Any>(val subject: T)

@Serializable
data class SubjectsWrapper<T : Any>(val subjects: T)

@Serializable
data class Subject(val id: Long, val name: String, val description: String, val language: String)

@Resource("/subjects")
data class SubjectsResource(val parent: RootResource = RootResource) {
    @Resource("/{id}")
    data class SubjectResource(val id: Long, val parent: SubjectsResource = SubjectsResource())
}


fun Route.subjectRoutes(subjectService: SubjectService, jwtService: JwtService) {
    get<SubjectsResource> {
        jwtAuth(jwtService) {
            either {
                val subjects = subjectService.getAllSubjects().bind()
                SubjectsWrapper(subjects.map {
                    Subject(
                        id = it.id.serial,
                        name = it.name,
                        description = it.description,
                        language = it.language,
                    )
                })
            }.respond(this, OK)
        }
    }

    get<SubjectsResource.SubjectResource> { resource ->
        jwtAuth(jwtService) {
            either {
                val subject = subjectService.getSubjectById(SubjectId(resource.id)).bind()
                SubjectWrapper(
                    Subject(
                        id = subject.id.serial,
                        name = subject.name,
                        description = subject.description,
                        language = subject.language
                    )
                )
            }.respond(this, OK)
        }
    }
}
