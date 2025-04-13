package com.github.laxy.service

import arrow.core.Either
import com.github.laxy.DomainError
import com.github.laxy.persistence.LanguageId
import com.github.laxy.persistence.LanguagePersistence
import com.github.laxy.util.withSpan

data class LanguageInfo(val id: LanguageId, val name: String, val code: String)

interface LanguageService {
    suspend fun getAllLanguages(): Either<DomainError, List<LanguageInfo>>
}

fun languageService(persistence: LanguagePersistence) =
    object : LanguageService {
        val spanPrefix = "LanguageService"

        override suspend fun getAllLanguages(): Either<DomainError, List<LanguageInfo>> =
            withSpan("$spanPrefix.getAllLanguages") {
                persistence.selectAll()
            }
    }
