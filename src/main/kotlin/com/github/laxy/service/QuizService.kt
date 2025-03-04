package com.github.laxy.service

import arrow.core.Either
import arrow.core.raise.either
import com.github.laxy.DomainError
import com.github.laxy.persistence.QuestionId
import com.github.laxy.persistence.QuestionOptionId
import com.github.laxy.persistence.QuizId
import com.github.laxy.persistence.QuizPersistence
import com.github.laxy.persistence.SubjectId
import com.github.laxy.persistence.SubjectPersistence
import com.github.laxy.persistence.UserId
import com.github.laxy.persistence.UserPersistence
import com.github.laxy.route.Quiz
import com.github.laxy.util.logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDateTime

@Serializable
data class ResponseQuestion(
    val description: String,
    val options: List<String>,
    val correctIndex: Int
)

data class CreateQuiz(val userId: UserId, val subjectId: SubjectId, val totalQuestions: Int)

data class QuizInfo(
    val id: QuizId,
    val subject: String,
    val totalQuestions: Int,
    val status: String,
    val createdAt: LocalDateTime
)

data class QuestionInfo(
    val id: QuestionId,
    val description: String,
    val options: List<OptionInfo>
)

data class OptionInfo(
    val id: QuestionOptionId,
    val description: String,
    val referenceNumber: Int,
    val isCorrect: Boolean
)

object QuizEvent {
    val eventChannel = MutableSharedFlow<Pair<QuizId, String>>(extraBufferCapacity = 100)
}

interface QuizService {
    suspend fun getByUser(userId: UserId): Either<DomainError, List<QuizInfo>>
    suspend fun getQuestionsByQuiz(quizId: QuizId): Either<DomainError, List<QuestionInfo>>
    suspend fun createQuiz(input: CreateQuiz): Either<DomainError, Quiz>
    suspend fun listenEvent(): Either<DomainError, Job>
}

fun quizService(
    userPersistence: UserPersistence,
    subjectPersistence: SubjectPersistence,
    quizPersistence: QuizPersistence,
    gptAIService: GptAIService,
    coroutineScope: CoroutineScope
) =
    object : QuizService {
        val log = logger()

        override suspend fun getByUser(userId: UserId): Either<DomainError, List<QuizInfo>> =
            quizPersistence.selectByUser(userId)

        override suspend fun getQuestionsByQuiz(quizId: QuizId): Either<DomainError, List<QuestionInfo>> =
            quizPersistence.selectQuestionsByQuiz(quizId)

        override suspend fun createQuiz(input: CreateQuiz): Either<DomainError, Quiz> = either {
            val subject = subjectPersistence.select(input.subjectId).bind()
            val currentTheme = userPersistence.selectCurrentTheme(input.userId).bind()
            val quizId = quizPersistence.insertQuiz(input.userId, input.subjectId, input.totalQuestions).bind()
            val message = """
                generate a multiple-choice quiz with exactly ${input.totalQuestions} questions about "${subject.name}" in "${subject.language}".
                each question must include:
                - `description` (String): The question text.
                - `options` (Array of 4 Strings): Four possible answers.
                - `correctIndex` (Integer, 0-3): The index of the correct answer.
                
                the quiz should focus on the theme: "${currentTheme.description}". Use relevant vocabulary and ensure engaging, informative content suitable for advanced learners.
                
                ### Output format:
                ONLY return a valid JSON array. Do NOT include extra text, explanations, or formatting.
                
                example:
                ```json
                [
                  {
                    "description": "What is 2 + 2?",
                    "options": ["3", "4", "5", "6"],
                    "correctIndex": 1
                  },
                  {
                    "description": "What is the capital of France?",
                    "options": ["Berlin", "Madrid", "Paris", "Lisbon"],
                    "correctIndex": 2
                  }
                ]
            """.trimIndent()
            coroutineScope.launch {
                val response = gptAIService.chatCompletion(ChatCompletionContent(message)).bind()
                val formattedResponse = response.replace(Regex("^```json|```$"), "").trim()
                QuizEvent.eventChannel.emit(quizId to formattedResponse)
            }
            Quiz(id = quizId.serial, totalQuestions = input.totalQuestions)
        }

        override suspend fun listenEvent() = either {
            coroutineScope.launch {
                QuizEvent.eventChannel.collect { (quizId, response) ->
                    Either.catch {
                        Json.decodeFromString<List<ResponseQuestion>>(response)
                    }.mapLeft { throwable ->
                        log.error("Error parsing GPT response for quiz ID: $quizId: ${throwable.message}")
                    }.map { questions ->
                        log.info("Processing GPT response for quiz ID: $quizId")
                        questions.forEach { question ->
                            val questionId = quizPersistence.insertQuestion(quizId, question.description).bind()
                            question.options.forEachIndexed { index, option ->
                                quizPersistence.insertQuestionOption(
                                    questionId,
                                    option,
                                    index,
                                    isCorrect = index == question.correctIndex
                                )
                            }
                        }
                        quizPersistence.updateStatus(quizId, "pending")
                        log.info("Successfully processed questions for quiz ID: $quizId")
                    }
                }
            }
        }
    }

