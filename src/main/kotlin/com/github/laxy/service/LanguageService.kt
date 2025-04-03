package com.github.laxy.service

import arrow.core.Either
import com.github.laxy.DomainError
import com.github.laxy.persistence.LanguageId
import com.github.laxy.persistence.LanguagePersistence
import io.opentelemetry.instrumentation.annotations.WithSpan

data class LanguageInfo(val id: LanguageId, val name: String, val code: String)

interface LanguageService {
    suspend fun getAllLanguages(): Either<DomainError, List<LanguageInfo>>
}

@WithSpan
fun languageService(persistence: LanguagePersistence) =
    object : LanguageService {
        override suspend fun getAllLanguages(): Either<DomainError, List<LanguageInfo>> =
            persistence.selectAll()
    }
