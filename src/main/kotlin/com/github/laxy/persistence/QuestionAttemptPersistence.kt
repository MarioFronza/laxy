package com.github.laxy.persistence

import com.github.laxy.service.QuestionAttempt
import com.github.laxy.sqldelight.QuestionAttemptsQueries
import com.github.laxy.util.withSpan

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
        val spanPrefix = "QuestionAttemptPersistence"

        override suspend fun selectQuestionAttemptsBy(questionId: QuestionId) =
            withSpan("$spanPrefix.selectQuestionAttemptsBy") {
                questionAttemptsQueries
                    .selectQuestionAttemptByQuestionId(questionId) { userSelectedOption, isCorrect
                        ->
                        QuestionAttempt(
                            id = questionId,
                            selectedOptionId = userSelectedOption,
                            isCorrect = isCorrect
                        )
                    }
                    .executeAsList()
            }

        override suspend fun insertQuestionAttempt(
            questionId: QuestionId,
            userSelectedOption: QuestionOptionId,
            isCorrect: Boolean
        ) =
            withSpan("$spanPrefix.insertQuestionAttempt") {
                questionAttemptsQueries.insertAttempt(
                    questionId = questionId,
                    userSelectedOption = userSelectedOption,
                    isCorrect = isCorrect
                )
            }
    }
