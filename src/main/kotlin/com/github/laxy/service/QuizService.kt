package com.github.laxy.service

import arrow.core.Either
import arrow.core.raise.either
import com.github.laxy.DomainError
import com.github.laxy.persistence.QuizId
import com.github.laxy.persistence.QuizPersistence
import com.github.laxy.persistence.SubjectId
import com.github.laxy.persistence.SubjectPersistence
import com.github.laxy.persistence.UserId
import com.github.laxy.persistence.UserPersistence
import com.github.laxy.route.Quiz
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ResponseQuestion(
    val description: String, val options: List<String>, val correctIndex: Int
)

data class CreateQuiz(val userId: UserId, val subjectId: SubjectId, val totalQuestions: Int)

data class SubjectInfo(val name: String, val description: String, val language: String)

object QuizEvent {
    val eventChannel = MutableSharedFlow<Pair<QuizId, String>>(extraBufferCapacity = 100)
}

interface QuizService {
    suspend fun createQuiz(input: CreateQuiz): Either<DomainError, Quiz>

    suspend fun listenEvent(): Either<DomainError, Job>
}

fun quizService(
    userPersistence: UserPersistence,
    subjectPersistence: SubjectPersistence,
    quizPersistence: QuizPersistence,
    gptAIService: GptAIService
) = object : QuizService {
    override suspend fun createQuiz(input: CreateQuiz): Either<DomainError, Quiz> = either {
        val subject = subjectPersistence.select(input.subjectId).bind()
        val currentTheme = userPersistence.selectCurrentTheme(input.userId).bind()
        val quizId = quizPersistence.insertQuiz(input.userId, input.subjectId, input.totalQuestions).bind()
        val message = """
        Generate a multiple-choice quiz with ${input.totalQuestions} questions about ${subject.name} - ${subject.language}. 
        Each question must have four answer choices, with one correct answer clearly indicated. 
        The quiz should focus on the user theme: ${currentTheme.description}. Make sure to incorporate relevant vocabulary and context whenever possible. 
        The format must be JSON, structured as an array of objects, 
        where each object represents a question with the following fields:
        - `description`: The question text.
        - `options`: A list of four possible answers.
        - `correctIndex`: The index of the correct answer (0 to 3).
        Ensure that the quiz is engaging, informative, and suitable for learners of ${subject.language} at a advanced proficiency level.
        """.trimIndent()
        CoroutineScope(Dispatchers.IO).launch {
            val response = gptAIService.chatCompletion(ChatCompletionContent(message)).bind()
            val formattedResponse = response.removePrefix("```json").removeSuffix("```").trim()
            QuizEvent.eventChannel.emit(quizId to formattedResponse)
        }
        Quiz(id = quizId.serial, totalQuestions = input.totalQuestions)
    }

    override suspend fun listenEvent() = either {
        CoroutineScope(Dispatchers.IO).launch {
            QuizEvent.eventChannel.collect { (quizId, response) ->
                val questions: List<ResponseQuestion> = Json.decodeFromString(response)
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
            }
        }
    }
}
