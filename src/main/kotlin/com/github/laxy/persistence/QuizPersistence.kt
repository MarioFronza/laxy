package com.github.laxy.persistence

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.github.laxy.DomainError
import com.github.laxy.QuestionCreationError
import com.github.laxy.QuestionOptionCreationError
import com.github.laxy.QuizCreationError
import com.github.laxy.service.OptionInfo
import com.github.laxy.service.QuestionInfo
import com.github.laxy.service.QuizInfo
import com.github.laxy.sqldelight.QuestionOptionsQueries
import com.github.laxy.sqldelight.QuestionsQueries
import com.github.laxy.sqldelight.QuizzesQueries
import com.github.laxy.util.withSpan
import io.opentelemetry.instrumentation.annotations.WithSpan

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

    suspend fun updateStatus(quizId: QuizId, status: String)
}

fun quizPersistence(
    quizzesQueries: QuizzesQueries,
    questionsQueries: QuestionsQueries,
    questionOptionsQueries: QuestionOptionsQueries
) =
    object : QuizPersistence {

        override suspend fun selectByUser(userId: UserId): Either<DomainError, List<QuizInfo>> =
            withSpan("QuizPersistence.selectByUser") {
                either {
                    quizzesQueries
                        .selectAll(userId) { id, name, totalQuestions, status, createdAt ->
                            QuizInfo(id, name, totalQuestions, status, createdAt)
                        }
                        .executeAsList()
                }
            }

        @WithSpan
        override suspend fun selectQuestionsByQuiz(
            quizId: QuizId
        ): Either<DomainError, List<QuestionInfo>> = either {
            questionsQueries
                .selectByQuiz(quizId) { id, description ->
                    val options =
                        questionOptionsQueries
                            .selectByQuestion(id) { optionId,
                                                    optionDescription,
                                                    referenceNumber,
                                                    isCorrect ->
                                OptionInfo(optionId, optionDescription, referenceNumber, isCorrect)
                            }
                            .executeAsList()
                    QuestionInfo(id, description, options)
                }
                .executeAsList()
        }

        @WithSpan
        override suspend fun selectOptionsByQuestion(
            questionId: QuestionId
        ): Either<DomainError, List<OptionInfo>> = either {
            questionOptionsQueries
                .selectByQuestion(questionId) { id, description, referenceNumber, isCorrect ->
                    OptionInfo(id, description, referenceNumber, isCorrect)
                }
                .executeAsList()
        }

        @WithSpan
        override suspend fun insertQuiz(
            userId: UserId,
            subjectId: SubjectId,
            totalQuestions: Int
        ): Either<DomainError, QuizId> = either {
            val quizId =
                quizzesQueries
                    .insertAndGetId(userId, subjectId, totalQuestions)
                    .executeAsOneOrNull()
            ensureNotNull(quizId) { QuizCreationError("quizId=$quizId") }
        }

        @WithSpan
        override suspend fun insertQuestion(
            quizId: QuizId,
            description: String
        ): Either<DomainError, QuestionId> = either {
            val questionId =
                questionsQueries.insertAndGetId(quizId, description).executeAsOneOrNull()
            ensureNotNull(questionId) { QuestionCreationError("questionId´=$quizId") }
        }

        @WithSpan
        override suspend fun insertQuestionOption(
            questionId: QuestionId,
            description: String,
            referenceNumber: Int,
            isCorrect: Boolean
        ): Either<DomainError, QuestionOptionId> = either {
            val questionOptionId =
                questionOptionsQueries
                    .insertAndGetId(questionId, description, referenceNumber, isCorrect)
                    .executeAsOneOrNull()
            ensureNotNull(questionOptionId) {
                QuestionOptionCreationError("questionOptionId=$questionOptionId")
            }
        }

        @WithSpan
        override suspend fun updateStatus(quizId: QuizId, status: String) {
            quizzesQueries.updateStatus(status, quizId)
        }
    }
