package com.github.laxy.persistence

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.github.laxy.DomainError
import com.github.laxy.QuizCreationError
import com.github.laxy.sqldelight.QuizzesQueries

@JvmInline value class QuizId(val serial: Long)

@JvmInline value class QuestionId(val serial: Long)

@JvmInline value class QuestionOptionId(val serial: Long)

@JvmInline value class QuestionAttemptId(val serial: Long)

interface QuizPersistence {
    suspend fun insert(
        userId: UserId,
        subjectId: SubjectId,
        totalQuestions: Int
    ): Either<DomainError, QuizId>
}

fun quizPersistence(
    quizzesQueries: QuizzesQueries
) = object : QuizPersistence {
    override suspend fun insert(
        userId: UserId,
        subjectId: SubjectId,
        totalQuestions: Int
    ): Either<DomainError, QuizId> = either {
        val quizId = quizzesQueries
            .insertAndGetId(userId.serial, subjectId.serial, totalQuestions)
            .executeAsOneOrNull()
        ensureNotNull(quizId) {
           QuizCreationError("quizId=$quizId")
        }
    }

}