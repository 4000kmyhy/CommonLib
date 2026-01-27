package com.yhy.lib_retrofit.entity

/**
 * desc:
 **
 * user: xujj
 * time: 2025/5/12 10:40
 **/
object DeepSeekChat {
    fun analyzeJson(result: String): String {
        var cleanedResult = result.trim()
        if (cleanedResult.startsWith("```json")) {
            cleanedResult = cleanedResult.substring(7).trim()
        }
        if (cleanedResult.endsWith("```")) {
            cleanedResult = cleanedResult.substring(0, cleanedResult.length - 3).trim()
        }
        return cleanedResult
    }

    fun ChatMessage.getResult() = analyzeJson(content)
}

data class ChatMessage(
    val role: String,
    val content: String
)

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 1.0,
    val max_tokens: Int = 1024
)

data class ChatResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: ChatMessage
)