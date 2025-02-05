package com.github.laxy.service

import arrow.core.Either
import arrow.core.raise.either
import com.github.laxy.DomainError
import com.github.laxy.persistence.SubjectId
import com.github.laxy.route.Quiz

data class CreateQuiz(
    val subjectId: SubjectId,
    val totalQuestions: Int
)

interface QuizService {
    suspend fun createQuiz(input: CreateQuiz): Either<DomainError, Quiz>
}

fun quizService(gptAIService: GptAIService) = object : QuizService {
    override suspend fun createQuiz(input: CreateQuiz): Either<DomainError, Quiz> = either {
        val message = """
        Generate a multiple-choice quiz with ${input.totalQuestions} questions about Present Simple in English - en. Each question must have four answer choices, with one correct answer clearly indicated. The quiz should focus on {theme}, incorporating relevant vocabulary and context whenever possible. The format must be JSON, structured as an array of objects, where each object represents a question with the following fields:
        - `description`: The question text.
        - `options`: A list of four possible answers.
        - `correctIndex`: The index of the correct answer (0 to 3).
        Ensure that the quiz is engaging, informative, and suitable for learners of {language} at a general proficiency level.
        """.trimIndent()
        val response = gptAIService.chatCompletion(ChatCompletionContent(message)).bind()
        println(response)
        Quiz(
            id = 1L,
            totalQuestions = input.totalQuestions
        )
    }
}