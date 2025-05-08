package com.github.laxy.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.cjcrafter.openai.chat.ChatMessage.Companion.toSystemMessage
import com.cjcrafter.openai.chat.chatRequest
import com.cjcrafter.openai.openAI
import com.github.laxy.DomainError
import com.github.laxy.InvalidIntegrationResponse
import com.github.laxy.util.withSpan

data class ChatCompletionContent(val message: String)

interface GptAIService {
    suspend fun chatCompletion(input: ChatCompletionContent): Either<DomainError, String>
}

class DefaultGptAIService(
    private val openAIKey: String,
) : GptAIService {

    private val spanPrefix = "GptAIService"
    private val openAI = openAI { apiKey(openAIKey) }

    override suspend fun chatCompletion(input: ChatCompletionContent): Either<DomainError, String> =
        withSpan(spanName = "$spanPrefix.chatCompletion") { span ->
            span.setAttribute("model", MODEL)
            either {
                val request = chatRequest {
                    model(MODEL)
                    addMessage(input.message.toSystemMessage())
                }
                val completion = openAI.createChatCompletion(request)[0]
                val content = completion.message.content
                ensureNotNull(content) { InvalidIntegrationResponse(content) }
                content
            }
        }

    companion object {
        private const val MODEL = "gpt-4.1"
    }
}

fun gptAIService(openAIKey: String): GptAIService = DefaultGptAIService(openAIKey)
