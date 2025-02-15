package com.github.laxy.service

import arrow.core.Either
import arrow.core.raise.either
import com.github.laxy.DomainError
import com.github.laxy.event.QuizEvent
import com.github.laxy.persistence.QuizPersistence
import com.github.laxy.persistence.SubjectId
import com.github.laxy.persistence.SubjectPersistence
import com.github.laxy.persistence.UserId
import com.github.laxy.persistence.UserPersistence
import com.github.laxy.route.Quiz
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class CreateQuiz(val userId: UserId, val subjectId: SubjectId, val totalQuestions: Int)

data class SubjectInfo(val name: String, val description: String, val language: String)

interface QuizService {
    suspend fun createQuiz(input: CreateQuiz): Either<DomainError, Quiz>

    suspend fun listenEvent()
}

fun quizService(
    userPersistence: UserPersistence,
    subjectPersistence: SubjectPersistence,
    quizPersistence: QuizPersistence,
    gptAIService: GptAIService
) =
    object : QuizService {
        override suspend fun createQuiz(input: CreateQuiz): Either<DomainError, Quiz> = either {
            val subject = subjectPersistence.select(input.subjectId).bind()
            val currentTheme = userPersistence.selectCurrentTheme(input.userId).bind()
            val quizId =
                quizPersistence.insert(input.userId, input.subjectId, input.totalQuestions).bind()
            val message =
                """
        Generate a multiple-choice quiz with ${input.totalQuestions} questions about ${subject.name} - ${subject.language}. 
        Each question must have four answer choices, with one correct answer clearly indicated. 
        The quiz should focus on the user theme: ${currentTheme.description}. Make sure to incorporate relevant vocabulary and context whenever possible. 
        The format must be JSON, structured as an array of objects, 
        where each object represents a question with the following fields:
        - `description`: The question text.
        - `options`: A list of four possible answers.
        - `correctIndex`: The index of the correct answer (0 to 3).
        Ensure that the quiz is engaging, informative, and suitable for learners of ${subject.language} at a advanced proficiency level.
        """
                    .trimIndent()
            CoroutineScope(Dispatchers.IO).launch {
                val response = gptAIService.chatCompletion(ChatCompletionContent(message)).bind()
                QuizEvent.eventChannel.emit(quizId to response)
            }
            Quiz(id = quizId.serial, totalQuestions = input.totalQuestions)
        }

        override suspend fun listenEvent() {
            CoroutineScope(Dispatchers.IO).launch {
                QuizEvent.eventChannel.collect { (quizId, response) ->
                    println(quizId)
                    println(response)
                }
            }
        }
    }
