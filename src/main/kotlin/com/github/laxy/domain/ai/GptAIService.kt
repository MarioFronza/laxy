package com.github.laxy.domain.ai

import com.cjcrafter.openai.chat.ChatMessage.Companion.toSystemMessage
import com.cjcrafter.openai.chat.chatRequest
import com.cjcrafter.openai.openAI
import com.github.laxy.domain.validation.notNull
import com.github.laxy.shared.InteractionResult
import com.github.laxy.shared.Success
import com.github.laxy.shared.interaction

data class ChatCompletionContent(val message: String)

interface GptAIService {
    suspend fun chatCompletion(input: ChatCompletionContent): InteractionResult<String>
}

fun gptAIService(openAIKey: String) =
    object : GptAIService {
        override suspend fun chatCompletion(
            input: ChatCompletionContent
        ): InteractionResult<String> = interaction {
            val openAI = openAI { apiKey(openAIKey) }
            val request = chatRequest {
                model("gpt-3.5-turbo")
                addMessage(input.message.toSystemMessage())
            }
            val completion = openAI.createChatCompletion(request)[0]
            val content = completion.message.content.notNull().bind()
            return Success(content)
        }
    }
