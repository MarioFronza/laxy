package com.github.laxy.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.cjcrafter.openai.chat.ChatMessage.Companion.toSystemMessage
import com.cjcrafter.openai.chat.chatRequest
import com.cjcrafter.openai.openAI
import com.github.laxy.DomainError
import com.github.laxy.InvalidIntegrationResponse
import io.opentelemetry.instrumentation.annotations.WithSpan

data class ChatCompletionContent(val message: String)

interface GptAIService {
    suspend fun chatCompletion(input: ChatCompletionContent): Either<DomainError, String>
}

@WithSpan
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
            val content = completion.message.content
            ensureNotNull(content) { InvalidIntegrationResponse(content) }
            content
        }
    }
