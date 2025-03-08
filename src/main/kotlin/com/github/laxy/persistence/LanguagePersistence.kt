package com.github.laxy.persistence

import arrow.core.Either
import arrow.core.raise.either
import com.github.laxy.DomainError
import com.github.laxy.service.LanguageInfo
import com.github.laxy.sqldelight.LanguagesQueries

@JvmInline value class LanguageId(val serial: Long)

interface LanguagePersistence {
    suspend fun selectAll(): Either<DomainError, List<LanguageInfo>>
}

fun languagePersistence(languagesQueries: LanguagesQueries) =
    object : LanguagePersistence {
        override suspend fun selectAll(): Either<DomainError, List<LanguageInfo>> = either {
            languagesQueries
                .selectAll { id, name, code -> LanguageInfo(id, name, code) }
                .executeAsList()
        }
    }
