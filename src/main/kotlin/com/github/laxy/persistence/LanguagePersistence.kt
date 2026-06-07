package com.github.laxy.persistence

import arrow.core.Either
import arrow.core.raise.either
import com.github.laxy.DomainError
import com.github.laxy.service.LanguageInfo
import com.github.laxy.sqldelight.LanguagesQueries
import com.github.laxy.util.withSpan

@JvmInline value class LanguageId(val serial: Long)

private const val spanPrefix = "persistence.language"

interface LanguagePersistence {
    suspend fun selectAll(): Either<DomainError, List<LanguageInfo>>
}

fun languagePersistence(languagesQueries: LanguagesQueries) =
    object : LanguagePersistence {

        override suspend fun selectAll(): Either<DomainError, List<LanguageInfo>> =
            withSpan("$spanPrefix.selectAll") {
                either {
                    languagesQueries
                        .selectAll { id, name, code -> LanguageInfo(id, name, code) }
                        .executeAsList()
                }
            }
    }
