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
import com.github.laxy.util.loadTemplate
import com.github.laxy.util.logger
import com.github.laxy.util.withSpan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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

data class QuestionInfo(val id: QuestionId, val description: String, val options: List<OptionInfo>)

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

    suspend fun getOptionsByQuestion(questionId: QuestionId): Either<DomainError, List<OptionInfo>>

    suspend fun createQuiz(input: CreateQuiz): Either<DomainError, Quiz>

    suspend fun listenEvent(): Job
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
        val spanPrefix = "QuizService"

        override suspend fun getByUser(userId: UserId): Either<DomainError, List<QuizInfo>> =
            withSpan(spanName = "$spanPrefix.getByUser") {
                quizPersistence.selectByUser(userId)
            }

        override suspend fun getQuestionsByQuiz(
            quizId: QuizId
        ): Either<DomainError, List<QuestionInfo>> =
            withSpan(spanName = "$spanPrefix.getQuestionsByQuiz") {
                quizPersistence.selectQuestionsByQuiz(quizId)
            }

        override suspend fun getOptionsByQuestion(
            questionId: QuestionId
        ): Either<DomainError, List<OptionInfo>> =
            withSpan(spanName = "$spanPrefix.getOptionsByQuestion") {
                quizPersistence.selectOptionsByQuestion(questionId)
            }

        override suspend fun createQuiz(input: CreateQuiz): Either<DomainError, Quiz> =
            withSpan(spanName = "$spanPrefix.createQuiz") {
                either {
                    val quizId = quizPersistence.insertQuiz(input.userId, input.subjectId, input.totalQuestions).bind()
                    val prompt = buildGptPrompt(input).bind()
                    coroutineScope.launch { emitGptPrompt(quizId, prompt).bind() }
                    Quiz(id = quizId.serial, totalQuestions = input.totalQuestions)
                }
            }

        private suspend fun buildGptPrompt(input: CreateQuiz): Either<DomainError, String> = either {
            val subject = subjectPersistence.select(input.subjectId).bind()
            val theme = userPersistence.selectCurrentTheme(input.userId).bind()
            val template = loadTemplate("prompts/quiz_template.txt")
            template
                .replace("{totalQuestions}", input.totalQuestions.toString())
                .replace("{subject}", subject.name)
                .replace("{language}", subject.language)
                .replace("{theme}", theme.description)
        }

        private suspend fun emitGptPrompt(quizId: QuizId, message: String): Either<DomainError, Unit> = either {
            val response = gptAIService.chatCompletion(ChatCompletionContent(message)).bind()
            val formattedResponse = response.replace(Regex("^```json|```$"), "").trim()
            QuizEvent.eventChannel.emit(quizId to formattedResponse)
        }

        override suspend fun listenEvent(): Job =
            coroutineScope.launch(SupervisorJob()) {
                QuizEvent.eventChannel.collect { (quizId, response) ->
                    handleEvent(quizId, response)
                }
            }

        private suspend fun handleEvent(quizId: QuizId, response: String) =
            withSpan(spanName = "[EVENT] - $spanPrefix.listenEvent") { span ->
                span.setAttribute("quiz.id", quizId.serial)

                val questionsResult = Either.catch {
                    Json.decodeFromString<List<ResponseQuestion>>(response)
                }

                questionsResult
                    .mapLeft { logParsingError(quizId, it) }
                    .map { processQuestions(quizId, it) }
            }

        private fun logParsingError(quizId: QuizId, throwable: Throwable) {
            log.error("Error parsing GPT response for quiz ID: $quizId: ${throwable.message}")
        }

        private suspend fun processQuestions(quizId: QuizId, questions: List<ResponseQuestion>) = either {
            log.info("Processing GPT response for quiz ID: $quizId")

            for (question in questions) {
                val questionId = quizPersistence
                    .insertQuestion(quizId, question.description)
                    .bind()

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
