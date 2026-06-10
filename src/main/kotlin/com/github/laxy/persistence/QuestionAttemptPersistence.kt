package com.github.laxy.persistence

import com.github.laxy.service.QuestionAttempt
import com.github.laxy.sqldelight.QuestionAttemptsQueries
import io.opentelemetry.instrumentation.annotations.WithSpan

@JvmInline value class QuestionAttemptId(val serial: Long)

interface QuestionAttemptPersistence {
    suspend fun selectQuestionAttemptsBy(questionId: QuestionId): List<QuestionAttempt>

    suspend fun insertQuestionAttempt(
        questionId: QuestionId,
        userSelectedOption: QuestionOptionId,
        isCorrect: Boolean
    )
}

fun questionAttemptPersistence(questionAttemptsQueries: QuestionAttemptsQueries) =
    object : QuestionAttemptPersistence {

        @WithSpan("QuestionAttemptPersistence.selectQuestionAttemptsBy")
        override suspend fun selectQuestionAttemptsBy(questionId: QuestionId) =
            questionAttemptsQueries
                .selectQuestionAttemptByQuestionId(questionId) { userSelectedOption, isCorrect ->
                    QuestionAttempt(
                        id = questionId,
                        selectedOptionId = userSelectedOption,
                        isCorrect = isCorrect
                    )
                }
                .executeAsList()

        @WithSpan("QuestionAttemptPersistence.insertQuestionAttempt")
        override suspend fun insertQuestionAttempt(
            questionId: QuestionId,
            userSelectedOption: QuestionOptionId,
            isCorrect: Boolean
        ) {
            questionAttemptsQueries.insertAttempt(
                questionId = questionId,
                userSelectedOption = userSelectedOption,
                isCorrect = isCorrect
            )
        }
    }
