package com.yhy.commonlib.api

import com.google.gson.JsonObject
import com.yhy.commonlib.entity.KieResponse
import com.yhy.commonlib.entity.KieTask
import com.yhy.commonlib.entity.ValidateInfo
import com.yhy.commonlib.entity.VoiceAvailable
import com.yhy.commonlib.entity.VoiceInfo
import com.yhy.lib_retrofit.utils.RetrofitUtils
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * desc:
 **
 * user: xujj
 * time: 2026/4/9 17:16
 **/
interface KieService {

    @POST("api/v1/voice/validate")
    suspend fun generateValidation(
        @Header("Authorization") authorization: String = "Bearer $apiKey",
        @Body request: JsonObject
    ): KieResponse<KieTask>

    @GET("api/v1/voice/validate-info")
    suspend fun getValidateInfo(
        @Header("Authorization") authorization: String = "Bearer $apiKey",
        @Query("taskId") taskId: String
    ): KieResponse<ValidateInfo>

    @POST("api/v1/voice/regenerate")
    suspend fun regenerateValidation(
        @Header("Authorization") authorization: String = "Bearer $apiKey",
        @Body request: JsonObject
    ): KieResponse<KieTask>

    @POST("api/v1/voice/generate")
    suspend fun generateVoice(
        @Header("Authorization") authorization: String = "Bearer $apiKey",
        @Body request: JsonObject
    ): KieResponse<KieTask>

    @GET("api/v1/voice/record-info")
    suspend fun getVoiceInfo(
        @Header("Authorization") authorization: String = "Bearer $apiKey",
        @Query("taskId") taskId: String
    ): KieResponse<VoiceInfo>

    @POST("api/v1/voice/check-voice")
    suspend fun checkVoice(
        @Header("Authorization") authorization: String = "Bearer $apiKey",
        @Body request: JsonObject
    ): KieResponse<VoiceAvailable>

    companion object {
        private const val baseUrl = "https://api.kie.ai/"
        private const val apiKey = "1216fd0aace4247b978ecaf490822e35"

        fun create(): KieService = RetrofitUtils
            .build(baseUrl)
            .create(KieService::class.java)
    }
}