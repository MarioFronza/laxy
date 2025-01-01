package com.github.laxy.domain.ai

import arrow.core.Either
import arrow.core.raise.either
import com.cjcrafter.openai.chat.ChatMessage.Companion.toSystemMessage
import com.cjcrafter.openai.chat.chatRequest
import com.cjcrafter.openai.openAI
import com.github.laxy.shared.DomainError

data class ChatCompletionContent(val message: String)

interface GptAIService {
    suspend fun chatCompletion(input: ChatCompletionContent): Either<DomainError, String>
}

fun gptAIService(openAIKey: String) =
    object : GptAIService {
        override suspend fun chatCompletion(
            input: ChatCompletionContent
        ): Either<DomainError, String> = either {
            val openAI = openAI { apiKey(openAIKey) }
            val request = chatRequest {
                model("gpt-3.5-turbo")
                addMessage(input.message.toSystemMessage())
            }
            val completion = openAI.createChatCompletion(request)[0]
            completion.message.content ?: throw Exception()
        }
    }
