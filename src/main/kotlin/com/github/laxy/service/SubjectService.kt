package com.github.laxy.service

import arrow.core.Either
import com.github.laxy.DomainError
import com.github.laxy.persistence.LanguageId
import com.github.laxy.persistence.SubjectId
import com.github.laxy.persistence.SubjectPersistence
import com.github.laxy.util.withSpan

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
        val spanPrefix = "SubjectService"

        override suspend fun getAllSubjects(): Either<DomainError, List<SubjectInfo>> =
            withSpan("$spanPrefix.getAllSubjects") {
                persistence.selectAll()
            }

        override suspend fun getAllSubjectsByLanguage(
            languageId: LanguageId
        ): Either<DomainError, List<SubjectInfo>> =
            withSpan("$spanPrefix.getAllSubjectsByLanguage") { span ->
                span.setAttribute("language.id", languageId.serial)
                persistence.selectByLanguage(languageId)
            }

        override suspend fun getSubjectById(id: SubjectId): Either<DomainError, SubjectInfo> =
            withSpan("$spanPrefix.getSubjectById") { span ->
                span.setAttribute("subject.id", id.serial)
                persistence.select(id)
            }
    }
