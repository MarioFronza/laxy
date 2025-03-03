package com.github.laxy.service

import arrow.core.Either
import com.github.laxy.DomainError
import com.github.laxy.persistence.SubjectId
import com.github.laxy.persistence.SubjectPersistence

data class SubjectInfo(
    val id: SubjectId,
    val name: String,
    val description: String,
    val language: String
)

interface SubjectService {
    suspend fun getAllSubjects(): Either<DomainError, List<SubjectInfo>>
    suspend fun getSubjectById(id: SubjectId): Either<DomainError, SubjectInfo>
}

fun subjectService(persistence: SubjectPersistence) =
    object : SubjectService {
        override suspend fun getAllSubjects(): Either<DomainError, List<SubjectInfo>> =
            persistence.selectAll()

        override suspend fun getSubjectById(id: SubjectId): Either<DomainError, SubjectInfo> =
            persistence.select(id)
    }