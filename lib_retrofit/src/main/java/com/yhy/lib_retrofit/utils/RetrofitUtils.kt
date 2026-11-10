package com.yhy.lib_retrofit.utils

import com.google.gson.GsonBuilder
import com.google.gson.Strictness
import com.google.gson.ToNumberPolicy
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * desc:
 **
 * user: xujj
 * time: 2025/3/18 9:14
 **/
object RetrofitUtils {

    private fun getOkHttpClient() = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    fun build(baseUrl: String): Retrofit {
        val gson = GsonBuilder()
            .setStrictness(Strictness.LENIENT)
            .disableHtmlEscaping()
            .setObjectToNumberStrategy(ToNumberPolicy.LAZILY_PARSED_NUMBER)
            .setNumberToNumberStrategy(ToNumberPolicy.LAZILY_PARSED_NUMBER)
            .create()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(getOkHttpClient())
            .build()
    }
}