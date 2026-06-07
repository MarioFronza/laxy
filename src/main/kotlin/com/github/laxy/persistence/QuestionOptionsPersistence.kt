package com.github.laxy.persistence

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.github.laxy.DomainError
import com.github.laxy.QuestionOptionCreationError
import com.github.laxy.service.OptionInfo
import com.github.laxy.sqldelight.QuestionOptionsQueries
import io.opentelemetry.instrumentation.annotations.WithSpan

@JvmInline value class QuestionOptionId(val serial: Long)

interface QuestionOptionsPersistence {
    suspend fun selectOptionsByQuestion(
        questionId: QuestionId
    ): Either<DomainError, List<OptionInfo>>

    suspend fun insertQuestionOption(
        questionId: QuestionId,
        description: String,
        referenceNumber: Int,
        isCorrect: Boolean
    ): Either<DomainError, QuestionOptionId>
}

fun questionOptionsPersistence(questionOptionsQueries: QuestionOptionsQueries) =
    object : QuestionOptionsPersistence {

        @WithSpan("QuestionOptionsPersistence.selectOptionsByQuestion")
        override suspend fun selectOptionsByQuestion(
            questionId: QuestionId
        ): Either<DomainError, List<OptionInfo>> = either {
            questionOptionsQueries
                .selectByQuestion(questionId) { id, description, referenceNumber, isCorrect ->
                    OptionInfo(id, description, referenceNumber, isCorrect)
                }
                .executeAsList()
        }

        @WithSpan("QuestionOptionsPersistence.insertQuestionOption")
        override suspend fun insertQuestionOption(
            questionId: QuestionId,
            description: String,
            referenceNumber: Int,
            isCorrect: Boolean
        ) = either {
            val questionOptionId =
                questionOptionsQueries
                    .insertAndGetId(questionId, description, referenceNumber, isCorrect)
                    .executeAsOneOrNull()
            ensureNotNull(questionOptionId) {
                QuestionOptionCreationError("questionOptionId=$questionOptionId")
            }
        }
    }
