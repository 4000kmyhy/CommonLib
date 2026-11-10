package com.yhy.commonlib.api

import com.google.gson.JsonObject
import com.yhy.commonlib.entity.UploadResponse
import com.yhy.lib_retrofit.utils.RetrofitUtils
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * desc:
 **
 * user: xujj
 * time: 2026/4/9 17:16
 **/
interface UploadService {

    @POST("api/file-url-upload")
    suspend fun uploadUrl(
        @Header("Content-Type") type: String = "application/json",
        @Header("Authorization") authorization: String = "Bearer $apiKey",
        @Body request: JsonObject
    ): UploadResponse

    @Multipart
    @POST("api/file-stream-upload")
    suspend fun uploadFile(
        @Header("Authorization") authorization: String = "Bearer $apiKey",
        @Part file: MultipartBody.Part,
        @Part uploadPath: MultipartBody.Part,
//        @Part fileName: MultipartBody.Part
    ): UploadResponse

    companion object {
        private const val baseUrl = "https://kieai.riftrunnerai.com/"
        private const val apiKey = "1216fd0aace4247b978ecaf490822e35"

        fun create(): UploadService = RetrofitUtils
            .build(baseUrl)
            .create(UploadService::class.java)
    }
}