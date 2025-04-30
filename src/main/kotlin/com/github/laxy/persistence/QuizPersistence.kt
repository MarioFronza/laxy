package com.github.laxy.persistence

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.github.laxy.DomainError
import com.github.laxy.QuizCreationError
import com.github.laxy.QuizSelectionError
import com.github.laxy.service.QuizInfo
import com.github.laxy.sqldelight.QuizzesQueries
import com.github.laxy.util.withSpan

@JvmInline value class QuizId(val serial: Long)

interface QuizPersistence {
    suspend fun selectByUser(userId: UserId): Either<DomainError, List<QuizInfo>>

    suspend fun selectById(quizId: QuizId): Either<DomainError, QuizInfo>

    suspend fun insertQuiz(
        userId: UserId,
        subjectId: SubjectId,
        totalQuestions: Int
    ): Either<DomainError, QuizId>

    suspend fun updateStatus(quizId: QuizId, status: String)

    suspend fun deleteQuiz(quizId: QuizId)
}

fun quizPersistence(quizzesQueries: QuizzesQueries) =
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

        override suspend fun selectById(quizId: QuizId) =
            withSpan("$spanPrefix.selectById") {
                either {
                    val quiz =
                        quizzesQueries
                            .selectById(quizId) { id, name, totalQuestions, status, createdAt ->
                                QuizInfo(id, name, totalQuestions, status, createdAt)
                            }
                            .executeAsOneOrNull()
                    ensureNotNull(quiz) { QuizSelectionError("quizId=$quizId") }
                }
            }

        override suspend fun insertQuiz(userId: UserId, subjectId: SubjectId, totalQuestions: Int) =
            withSpan("$spanPrefix.insertQuiz") {
                either {
                    val quizId =
                        quizzesQueries
                            .insertAndGetId(userId, subjectId, totalQuestions)
                            .executeAsOneOrNull()
                    ensureNotNull(quizId) { QuizCreationError("quizId=$quizId") }
                }
            }

        override suspend fun updateStatus(quizId: QuizId, status: String) =
            withSpan("$spanPrefix.updateStatus") { quizzesQueries.updateStatus(status, quizId) }

        override suspend fun deleteQuiz(quizId: QuizId) =
            withSpan("$spanPrefix.deleteQuiz") { quizzesQueries.deleteById(quizId) }
    }
