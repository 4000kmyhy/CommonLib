package com.yhy.lib_retrofit.service

import android.util.Log
import com.yhy.lib_retrofit.entity.ChatMessage
import com.yhy.lib_retrofit.entity.ChatRequest
import com.yhy.lib_retrofit.entity.ChatResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * desc:
 **
 * user: xujj
 * time: 2025/5/12 10:41
 **/
interface DeepSeekApiService {

    companion object {
        private const val baseUrl = "https://api.deepseek.com"

        private val mService by lazy {
            Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(DeepSeekApiService::class.java)
        }

        val system_prompt =
            "请你扮演一个刚从美国留学回国的人，说话时候会故意中文夹杂部分英文单词，显得非常fancy，对话中总是带有很强的优越感。"
        val user_prompt = "美国的饮食还习惯么。"

        suspend fun request(content: String) {
            try {
                Log.d("xxx", "request: " + mService)
                val response = mService.chatCompletion(
                    apiKey = "Bearer sk-f7796657bf404df1ae877b049cec799e",
                    request = ChatRequest(
                        model = "deepseek-chat",
                        messages = listOf(
                            ChatMessage("system", system_prompt),
                            ChatMessage("user", user_prompt),
                        )
                    )
                )
                Log.d("xxx", "request: " + response)
//                if (response.isSuccessful) {
//                    Log.d("xxx", "request1: " + response.body().toString())
////                    val reply = response.body()?.choices?.first()?.message?.content
////                    textViewReply.text = reply ?: "No response"
//                } else {
//                    Log.d("xxx", "request2: " + response.errorBody()?.string())
////                    textViewReply.text = "Error: ${response.errorBody()?.string()}"
//                }
            } catch (e: Exception) {
//                textViewReply.text = "Error: ${e.message}"
                Log.d("xxx", "request3: e=" + e)
            }
        }
    }

    @POST("chat/completions")
    suspend fun chatCompletion(
        @Header("Content-Type") type: String = "application/json",
        @Header("Authorization") apiKey: String,
        @Body request: ChatRequest
    ): ChatResponse
}