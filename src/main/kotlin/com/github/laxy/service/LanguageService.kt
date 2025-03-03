package com.github.laxy.service

import arrow.core.Either
import com.github.laxy.DomainError
import com.github.laxy.persistence.LanguageId
import com.github.laxy.persistence.LanguagePersistence

data class LanguageInfo(
    val id: LanguageId,
    val name: String,
    val code: String
)

interface LanguageService {
    suspend fun getAllLanguages(): Either<DomainError, List<LanguageInfo>>
}

fun languageService(persistence: LanguagePersistence) =
    object : LanguageService {
        override suspend fun getAllLanguages(): Either<DomainError, List<LanguageInfo>> =
            persistence.selectAll()
    }