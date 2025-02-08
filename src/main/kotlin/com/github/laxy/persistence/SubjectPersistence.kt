package com.github.laxy.persistence

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.github.laxy.DomainError
import com.github.laxy.SubjectNotFound
import com.github.laxy.service.SubjectInfo
import com.github.laxy.sqldelight.SubjectsQueries

@JvmInline
value class LanguageId(val serial: Long)

@JvmInline
value class SubjectId(val serial: Long)

interface SubjectPersistence {
    suspend fun select(subjectId: SubjectId): Either<DomainError, SubjectInfo>
}

fun subjectPersistence(subjectsQueries: SubjectsQueries) = object : SubjectPersistence {
    override suspend fun select(subjectId: SubjectId): Either<DomainError, SubjectInfo> = either {
        val subjectInfo = subjectsQueries.selectById(subjectId) { name, description, language ->
            SubjectInfo(
                name, description, language
            )
        }.executeAsOneOrNull()
        ensureNotNull(subjectInfo) { SubjectNotFound("subjectId=$subjectId") }
    }
}
