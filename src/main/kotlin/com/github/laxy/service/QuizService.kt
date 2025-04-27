package com.github.laxy.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.github.laxy.DomainError
import com.github.laxy.QuizAttemptError
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
import io.opentelemetry.api.trace.StatusCode.ERROR
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
    val options: List<OptionInfo>,
    val lastAttempt: QuestionAttempt?
)

data class OptionInfo(
    val id: QuestionOptionId,
    val description: String,
    val referenceNumber: Int,
    val isCorrect: Boolean
)

data class QuizAttempt(
    val quizId: QuizId,
    val questions: List<QuestionAttempt>
)

data class QuestionAttempt(
    val id: QuestionId,
    val selectedOptionId: QuestionOptionId,
    val isCorrect: Boolean
)

data class QuizAttemptOutput(
    val questions: List<QuestionAttemptOutput>
)

data class QuestionAttemptOutput(
    val userOptionId: QuestionOptionId,
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

    suspend fun quizAttempt(input: QuizAttempt): Either<DomainError, QuizAttemptOutput>

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
            withSpan(spanName = "$spanPrefix.getByUser") { quizPersistence.selectByUser(userId) }

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
                    val quizId =
                        quizPersistence
                            .insertQuiz(input.userId, input.subjectId, input.totalQuestions)
                            .bind()
                    val prompt = buildGptPrompt(input).bind()
                    coroutineScope.launch { emitGptPrompt(quizId, prompt).bind() }
                    Quiz(id = quizId.serial, totalQuestions = input.totalQuestions)
                }
            }

        override suspend fun quizAttempt(input: QuizAttempt): Either<DomainError, QuizAttemptOutput> =
            withSpan(spanName = "$spanPrefix.quizAttempt") { span ->
                either {
                    val quizId = input.quizId
                    span.setAttribute("quiz.id", quizId.serial)

                    val persistedQuestions = quizPersistence.selectQuestionsByQuiz(input.quizId).bind()

                    ensure(input.questions.size == persistedQuestions.size) {
                        span.setStatus(ERROR)
                        QuizAttemptError(
                            "Mismatch between input questions (${input.questions.size}) and persisted questions (${persistedQuestions.size}) for quizId: ${input.quizId.serial}"
                        )
                    }

                    val questionsById = persistedQuestions.associateBy { it.id }

                    val questionAttempts = input.questions.map { questionAttempt ->
                        val question = questionsById[questionAttempt.id]
                        val correctOption = question?.options?.firstOrNull { it.isCorrect }

                        ensureNotNull(correctOption) {
                            span.setStatus(ERROR)
                            QuizAttemptError("Could not find correct option for questionId: ${questionAttempt.id.serial}")
                        }

                        quizPersistence.updateStatus(quizId, "completed")

                        QuestionAttemptOutput(
                            userOptionId = questionAttempt.selectedOptionId,
                            isCorrect = questionAttempt.selectedOptionId == correctOption.id
                        )
                    }

                    QuizAttemptOutput(questionAttempts)
                }
            }

        private suspend fun buildGptPrompt(input: CreateQuiz): Either<DomainError, String> =
            either {
                val subject = subjectPersistence.select(input.subjectId).bind()
                val theme = userPersistence.selectCurrentTheme(input.userId).bind()
                val template = loadTemplate("prompts/quiz_template.txt")
                template
                    .replace("{totalQuestions}", input.totalQuestions.toString())
                    .replace("{subject}", subject.name)
                    .replace("{language}", subject.language)
                    .replace("{theme}", theme.description)
            }

        private suspend fun emitGptPrompt(
            quizId: QuizId,
            message: String
        ): Either<DomainError, Unit> = either {
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

                val questionsResult =
                    Either.catch { Json.decodeFromString<List<ResponseQuestion>>(response) }

                questionsResult
                    .mapLeft { processEventError(quizId, it) }
                    .map { processQuestions(quizId, it) }
            }

        private suspend fun processEventError(quizId: QuizId, throwable: Throwable) {
            quizPersistence.deleteQuiz(quizId)
            log.error("Error parsing GPT response for quiz ID: $quizId: ${throwable.message}")
        }


        suspend fun processQuestions(quizId: QuizId, questions: List<ResponseQuestion>): Either<DomainError, Unit> =
            either {
                log.info("Processing GPT response for quiz ID: $quizId")

                coroutineScope {
                    questions.map { question ->
                        async {
                            insertQuestionWithOptions(quizId, question).bind()
                        }
                    }.map { it.await() }
                }

                quizPersistence.updateStatus(quizId, "pending")
                log.info("Successfully processed questions for quiz ID: $quizId")
            }

        private suspend fun insertQuestionWithOptions(
            quizId: QuizId,
            question: ResponseQuestion
        ): Either<DomainError, Unit> = either {
            val questionId = quizPersistence.insertQuestion(quizId, question.description).bind()

            question.options.forEachIndexed { index, option ->
                insertOption(questionId, option, index, index == question.correctIndex).bind()
            }
        }

        private suspend fun insertOption(
            questionId: QuestionId,
            option: String,
            index: Int,
            isCorrect: Boolean
        ): Either<DomainError, Unit> = either {
            quizPersistence.insertQuestionOption(questionId, option, index, isCorrect)
        }

    }

