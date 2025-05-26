package com.yhy.lib_web_pic.utils

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * desc:
 **
 * user: xujj
 * time: 2025/4/25 15:23
 **/
object RetrofitUtils {
    fun build(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(8000, TimeUnit.MILLISECONDS)
                    .readTimeout(8000, TimeUnit.MILLISECONDS)
                    .writeTimeout(8000, TimeUnit.MILLISECONDS)
                    .build()
            )
            .build()
    }
}