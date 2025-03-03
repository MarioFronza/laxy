package com.github.laxy.persistence

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.github.laxy.DomainError
import com.github.laxy.SubjectNotFound
import com.github.laxy.service.SubjectInfo
import com.github.laxy.sqldelight.SubjectsQueries


@JvmInline
value class SubjectId(val serial: Long)

interface SubjectPersistence {
    suspend fun selectAll(): Either<DomainError, List<SubjectInfo>>
    suspend fun selectByLanguage(languageId: LanguageId): Either<DomainError, List<SubjectInfo>>
    suspend fun select(subjectId: SubjectId): Either<DomainError, SubjectInfo>
}

fun subjectPersistence(subjectsQueries: SubjectsQueries) = object : SubjectPersistence {
    override suspend fun selectAll(): Either<DomainError, List<SubjectInfo>> = either {
        subjectsQueries.selectAll { id, name, description, language ->
            SubjectInfo(id, name, description, language)
        }.executeAsList()
    }

    override suspend fun selectByLanguage(languageId: LanguageId): Either<DomainError, List<SubjectInfo>> = either {
        subjectsQueries.selectSubjectsByLanguage(languageId) { id, name, description, language ->
            SubjectInfo(id, name, description, language)
        }.executeAsList()
    }

    override suspend fun select(subjectId: SubjectId): Either<DomainError, SubjectInfo> = either {
        val subjectInfo = subjectsQueries.selectById(subjectId) { id, name, description, language ->
            SubjectInfo(id, name, description, language)
        }.executeAsOneOrNull()
        ensureNotNull(subjectInfo) { SubjectNotFound("subjectId=${subjectId.serial}") }
    }
}
