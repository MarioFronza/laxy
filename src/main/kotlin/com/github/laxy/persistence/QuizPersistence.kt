package com.github.laxy.persistence

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.github.laxy.DomainError
import com.github.laxy.QuestionCreationError
import com.github.laxy.QuestionOptionCreationError
import com.github.laxy.QuizCreationError
import com.github.laxy.QuizSelectionError
import com.github.laxy.service.OptionInfo
import com.github.laxy.service.QuestionAttempt
import com.github.laxy.service.QuestionInfo
import com.github.laxy.service.QuizInfo
import com.github.laxy.sqldelight.QuestionAttemptsQueries
import com.github.laxy.sqldelight.QuestionOptionsQueries
import com.github.laxy.sqldelight.QuestionsQueries
import com.github.laxy.sqldelight.QuizzesQueries
import com.github.laxy.util.withSpan

@JvmInline
value class QuizId(val serial: Long)

@JvmInline
value class QuestionId(val serial: Long)

@JvmInline
value class QuestionOptionId(val serial: Long)

@JvmInline
value class QuestionAttemptId(val serial: Long)

interface QuizPersistence {

    suspend fun selectByUser(userId: UserId): Either<DomainError, List<QuizInfo>>

    suspend fun selectQuestionsByQuiz(quizId: QuizId): Either<DomainError, List<QuestionInfo>>

    suspend fun selectOptionsByQuestion(
        questionId: QuestionId
    ): Either<DomainError, List<OptionInfo>>

    suspend fun selectById(quizId: QuizId): Either<DomainError, QuizInfo>

    suspend fun selectQuestionAttemptsBy(questionId: QuestionId): Either<DomainError, List<QuestionAttempt>>

    suspend fun insertQuiz(
        userId: UserId,
        subjectId: SubjectId,
        totalQuestions: Int
    ): Either<DomainError, QuizId>

    suspend fun insertQuestion(quizId: QuizId, description: String): Either<DomainError, QuestionId>

    suspend fun insertQuestionOption(
        questionId: QuestionId,
        description: String,
        referenceNumber: Int,
        isCorrect: Boolean
    ): Either<DomainError, QuestionOptionId>

    suspend fun insertQuestionAttempt(questionId: QuestionId, userAnswer: Int, isCorrect: Boolean)

    suspend fun updateStatus(quizId: QuizId, status: String)

    suspend fun deleteQuiz(quizId: QuizId)
}

fun quizPersistence(
    quizzesQueries: QuizzesQueries,
    questionsQueries: QuestionsQueries,
    questionOptionsQueries: QuestionOptionsQueries,
    questionAttemptsQueries: QuestionAttemptsQueries
) =
    object : QuizPersistence {
        val spanPrefix = "QuizPersistence"

        override suspend fun selectByUser(userId: UserId): Either<DomainError, List<QuizInfo>> =
            withSpan("$spanPrefix.selectByUser") {
                either {
                    quizzesQueries
                        .selectAll(userId) { id, name, totalQuestions, status, createdAt ->
                            QuizInfo(id, name, totalQuestions, status, createdAt)
                        }
                        .executeAsList()
                }
            }

        override suspend fun selectQuestionsByQuiz(
            quizId: QuizId
        ): Either<DomainError, List<QuestionInfo>> =
            withSpan("$spanPrefix.selectQuestionsByQuiz") {
                either {
                    questionsQueries
                        .selectByQuiz(quizId) { id, description ->
                            val options =
                                questionOptionsQueries
                                    .selectByQuestion(id) { optionId,
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
                            QuestionInfo(id, description, options)
                        }
                        .executeAsList()
                }
            }

        override suspend fun selectOptionsByQuestion(
            questionId: QuestionId
        ): Either<DomainError, List<OptionInfo>> =
            withSpan("$spanPrefix.selectOptionsByQuestion") {
                either {
                    questionOptionsQueries
                        .selectByQuestion(questionId) { id, description, referenceNumber, isCorrect
                            ->
                            OptionInfo(id, description, referenceNumber, isCorrect)
                        }
                        .executeAsList()
                }
            }

        override suspend fun selectById(quizId: QuizId): Either<DomainError, QuizInfo> =
            withSpan("$spanPrefix.selectById") {
                either {
                    val quiz = quizzesQueries.selectById(quizId) { id, name, totalQuestions, status, createdAt ->
                        QuizInfo(
                            id = id,
                            subject = name,
                            status = status,
                            totalQuestions = totalQuestions,
                            createdAt = createdAt
                        )
                    }.executeAsOneOrNull()
                    ensureNotNull(quiz) { QuizSelectionError("quizId=$quizId") }
                }
            }

        override suspend fun selectQuestionAttemptsBy(questionId: QuestionId): Either<DomainError, List<QuestionAttempt>> =
            withSpan("$spanPrefix.selectQuestionAttemptsBy") {
                either {
                    questionAttemptsQueries.selectQuestionAttemptByQuestionId(questionId) { _, _, userAnswer ->
                        QuestionAttempt(
                            id = questionId,
                            selectedOptionId = QuestionOptionId(userAnswer.toLong())
                        )
                    }.executeAsList()

                }
            }

        override suspend fun insertQuiz(
            userId: UserId,
            subjectId: SubjectId,
            totalQuestions: Int
        ): Either<DomainError, QuizId> =
            withSpan("$spanPrefix.insertQuiz") {
                either {
                    val quizId =
                        quizzesQueries
                            .insertAndGetId(userId, subjectId, totalQuestions)
                            .executeAsOneOrNull()
                    ensureNotNull(quizId) { QuizCreationError("quizId=$quizId") }
                }
            }

        override suspend fun insertQuestion(
            quizId: QuizId,
            description: String
        ): Either<DomainError, QuestionId> =
            withSpan("$spanPrefix.insertQuestion") {
                either {
                    val questionId =
                        questionsQueries.insertAndGetId(quizId, description).executeAsOneOrNull()
                    ensureNotNull(questionId) { QuestionCreationError("questionId´=$quizId") }
                }
            }

        override suspend fun insertQuestionOption(
            questionId: QuestionId,
            description: String,
            referenceNumber: Int,
            isCorrect: Boolean
        ): Either<DomainError, QuestionOptionId> =
            withSpan("$spanPrefix.insertQuestionOption") {
                either {
                    val questionOptionId =
                        questionOptionsQueries
                            .insertAndGetId(questionId, description, referenceNumber, isCorrect)
                            .executeAsOneOrNull()
                    ensureNotNull(questionOptionId) {
                        QuestionOptionCreationError("questionOptionId=$questionOptionId")
                    }
                }
            }

        override suspend fun insertQuestionAttempt(questionId: QuestionId, userAnswer: Int, isCorrect: Boolean) {
            withSpan("$spanPrefix.insertQuestionAttempt") {
                questionAttemptsQueries.insertAttempt(
                    questionId = questionId,
                    userAnswer = userAnswer,
                    isCorrect = if (isCorrect) 1 else 0
                )
            }
        }

        override suspend fun updateStatus(quizId: QuizId, status: String) {
            withSpan("$spanPrefix.updateStatus") {
                quizzesQueries.updateStatus(status, quizId)
            }
        }

        override suspend fun deleteQuiz(quizId: QuizId) {
            withSpan("$spanPrefix.deleteQuiz") {
                quizzesQueries.deleteById(quizId)
            }
        }
    }
