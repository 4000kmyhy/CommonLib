package com.yhy.lib_retrofit.entity

/**
 * desc:
 **
 * user: xujj
 * time: 2025/5/12 10:40
 **/
class DeepSeekChat {
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