package com.github.laxy.persistence

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.github.laxy.DomainError
import com.github.laxy.QuestionCreationError
import com.github.laxy.QuestionOptionCreationError
import com.github.laxy.QuizCreationError
import com.github.laxy.sqldelight.QuestionOptionsQueries
import com.github.laxy.sqldelight.QuestionsQueries
import com.github.laxy.sqldelight.QuizzesQueries

@JvmInline value class QuizId(val serial: Long)

@JvmInline value class QuestionId(val serial: Long)

@JvmInline value class QuestionOptionId(val serial: Long)

@JvmInline value class QuestionAttemptId(val serial: Long)

interface QuizPersistence {
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
}

fun quizPersistence(
    quizzesQueries: QuizzesQueries,
    questionsQueries: QuestionsQueries,
    questionOptionsQueries: QuestionOptionsQueries
) =
    object : QuizPersistence {
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

        override suspend fun insertQuestion(
            quizId: QuizId,
            description: String
        ): Either<DomainError, QuestionId> = either {
            val questionId =
                questionsQueries.insertAndGetId(quizId, description).executeAsOneOrNull()
            ensureNotNull(questionId) { QuestionCreationError("questionId´=$quizId") }
        }

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
    }
