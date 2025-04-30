package com.github.laxy.persistence

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.github.laxy.DomainError
import com.github.laxy.QuestionCreationError
import com.github.laxy.service.OptionInfo
import com.github.laxy.service.QuestionAttempt
import com.github.laxy.service.QuestionInfo
import com.github.laxy.sqldelight.QuestionAttemptsQueries
import com.github.laxy.sqldelight.QuestionOptionsQueries
import com.github.laxy.sqldelight.QuestionsQueries
import com.github.laxy.util.withSpan

@JvmInline value class QuestionId(val serial: Long)

interface QuestionPersistence {
    suspend fun selectQuestionsByQuiz(quizId: QuizId): Either<DomainError, List<QuestionInfo>>

    suspend fun insertQuestion(quizId: QuizId, description: String): Either<DomainError, QuestionId>
}

fun questionPersistence(
    questionsQueries: QuestionsQueries,
    questionOptionsQueries: QuestionOptionsQueries,
    questionAttemptsQueries: QuestionAttemptsQueries
) =
    object : QuestionPersistence {
        val spanPrefix = "QuestionPersistence"

        override suspend fun selectQuestionsByQuiz(
            quizId: QuizId
        ): Either<DomainError, List<QuestionInfo>> =
            withSpan("$spanPrefix.selectQuestionsByQuiz") {
                either {
                    questionsQueries
                        .selectByQuiz(quizId) { id, description ->
                            val options =
                                questionOptionsQueries
                                    .selectByQuestion(id) {
                                        optionId,
                                        optionDescription,
                                        referenceNumber,
                                        isCorrect ->
                                        OptionInfo(
                                            optionId,
                                            optionDescription,
                                            referenceNumber,
                                            isCorrect
                                        )
                                    }
                                    .executeAsList()

                            val lastAttempt =
                                questionAttemptsQueries
                                    .selectLastAttemptByQuestionId(id) {
                                        userSelectedOption,
                                        isCorrect ->
                                        QuestionAttempt(
                                            id = id,
                                            selectedOptionId = userSelectedOption,
                                            isCorrect = isCorrect
                                        )
                                    }
                                    .executeAsOneOrNull()

                            QuestionInfo(id, description, options, lastAttempt)
                        }
                        .executeAsList()
                }
            }

        override suspend fun insertQuestion(quizId: QuizId, description: String) =
            withSpan("$spanPrefix.insertQuestion") {
                either {
                    val questionId =
                        questionsQueries.insertAndGetId(quizId, description).executeAsOneOrNull()
                    ensureNotNull(questionId) { QuestionCreationError("quizId=$quizId") }
                }
            }
    }
