package com.yhy.lib_retrofit.service

import android.util.Log
import com.yhy.lib_retrofit.entity.ChatMessage
import com.yhy.lib_retrofit.entity.ChatRequest
import com.yhy.lib_retrofit.entity.ChatResponse
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

/**
 * desc:
 **
 * user: xujj
 * time: 2025/5/12 10:41
 **/
interface GiteeAiApiService {

    companion object {
        /**
         * model:
         * InternVL2-8B
         * Qwen2-Audio-7B-Instruct
         * DeepSeek-Prover-V2-7B
         * InternVL3-38B
         * ERNIE-4.5-Turbo
         * InternVL3-78B
         * Lingshu-32B
         * HealthGPT-L14
         * DeepSeek-R1
         * Qwen3-4B
         * ERNIE-4.5-Turbo-VL
         * Qwen3-8B
         * Qwen3-14B
         * GLM-4-9B-0414
         * Qwen2-72B-Instruct
         * Qwen2-7B-Instruct
         * DeepSeek-V3
         * DeepSeek-R1-Distill-Qwen-14B
         * Qwen2.5-14B-Instruct
         * medgemma-4b-it
         * DeepSeek-R1-Distill-Qwen-1.5B
         * Qwen2.5-VL-32B-Instruct
         * Qwen2.5-32B-Instruct
         * Fin-R1
         * codegeex4-all-9b
         * Qwen2.5-Coder-32B-Instruct
         * DianJin-R1-32B
         * QwQ-32B
         * ERNIE-X1-Turbo
         * Qwen3-0.6B
         * kimi-k2-instruct
         * Yi-34B-Chat
         * InternVL2.5-78B
         * InternVL2.5-26B
         * Qwen2-VL-72B
         * internlm3-8b-instruct
         * glm-4-9b-chat
         * Align-DS-V
         * Qwen2.5-7B-Instruct
         * DeepSeek-R1-Distill-Qwen-7B
         * Qwen3-235B-A22B
         * gemma-3-27b-it
         * DeepSeek-R1-Distill-Qwen-32B
         * Qwen3-32B
         * deepseek-coder-33B-instruct
         * Qwen2.5-72B-Instruct
         * GLM-4-32B
         * Qwen2.5-Coder-14B-Instruct
         * Qwen3-30B-A3B
         * code-raccoon-v1
         */
        private const val TAG = "GiteeAi"
        private const val baseUrl = "https://ai.gitee.com/v1/"

        private val mService by lazy {
            Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(
                    OkHttpClient.Builder()
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .writeTimeout(15, TimeUnit.SECONDS)
                        .build()
                )
                .build()
                .create(GiteeAiApiService::class.java)
        }

        suspend fun request(
            model: String = "DeepSeek-V3",
            apiKey: String,
            messages: List<ChatMessage>,
            onResponse: (ChatResponse) -> Unit,
            onError: (String?) -> Unit
        ) {
            Log.d(TAG, "request: $messages")
            try {
                val response = mService.chat(
                    authorization = "Bearer $apiKey",
                    request = ChatRequest(
                        model = model,
                        messages = messages,
                        temperature = 0.1//降低随机性，确保稳定输出
                    )
                )
                Log.d(TAG, "request: messages=${response.choices}")
                onResponse(response)
            } catch (e: Exception) {
                Log.d(TAG, "request: e=$e")
                onError(e.message)
            }
        }

        suspend fun request(
            model: String = "DeepSeek-V3",
            apiKey: String,
            prompt: String,
            onResponse: (ChatResponse) -> Unit,
            onError: (String?) -> Unit
        ) {
            Log.d(TAG, "request: $prompt")
            try {
                val response = mService.chat(
                    authorization = "Bearer $apiKey",
                    request = ChatRequest(
                        model = model,
                        messages = listOf(ChatMessage(role = "user", content = prompt))
                    )
                )
                Log.d(TAG, "request: messages=${response.choices}")
                onResponse(response)
            } catch (e: Exception) {
                Log.d(TAG, "request: e=$e")
                onError(e.message)
            }
        }
    }

    @POST("chat/completions")
    suspend fun chat(
        @Header("Content-Type") type: String = "application/json",
        @Header("Authorization") authorization: String,
        @Body request: ChatRequest
    ): ChatResponse
}