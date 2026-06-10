package com.github.laxy.service

import arrow.core.Either
import com.github.laxy.DomainError
import com.github.laxy.persistence.LanguageId
import com.github.laxy.persistence.SubjectId
import com.github.laxy.persistence.SubjectPersistence
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan

data class SubjectInfo(
    val id: SubjectId,
    val name: String,
    val description: String,
    val language: String
)

interface SubjectService {
    suspend fun getAllSubjects(): Either<DomainError, List<SubjectInfo>>

    suspend fun getAllSubjectsByLanguage(
        languageId: LanguageId
    ): Either<DomainError, List<SubjectInfo>>

    suspend fun getSubjectById(id: SubjectId): Either<DomainError, SubjectInfo>
}

fun subjectService(persistence: SubjectPersistence) =
    object : SubjectService {

        @WithSpan("SubjectService.getAllSubjects")
        override suspend fun getAllSubjects(): Either<DomainError, List<SubjectInfo>> =
            persistence.selectAll()

        @WithSpan("SubjectService.getAllSubjectsByLanguage")
        override suspend fun getAllSubjectsByLanguage(
            languageId: LanguageId
        ): Either<DomainError, List<SubjectInfo>> {
            Span.current().setAttribute("language.id", languageId.serial)
            return persistence.selectByLanguage(languageId)
        }

        @WithSpan("SubjectService.getSubjectById")
        override suspend fun getSubjectById(id: SubjectId): Either<DomainError, SubjectInfo> {
            Span.current().setAttribute("subject.id", id.serial)
            return persistence.select(id)
        }
    }
