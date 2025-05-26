package com.yhy.lib_retrofit.service

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * desc:
 **
 * user: xujj
 * time: 2025/3/17 14:31
 **/
interface ApiService {
    companion object {
        inline fun <reified T : ApiService> create(baseUrl: String): T {
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
                .create(T::class.java)
        }
    }
}