package com.yhy.lib_retrofit.service

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * desc:
 **
 * user: xujj
 * time: 2025/4/3 17:34
 **/
interface LyricApiService : ApiService {

    companion object {
        private const val baseUrl = "https://c.y.qq.com/lyric/fcgi-bin/"

        private fun create(): LyricApiService {
            return ApiService.create(baseUrl)
        }
    }

    @GET("fcg_query_lyric_new.fcg")
    suspend fun getLyric(
        @Query("format") format: String = "json",
        @Query("songmid") songmid: String
    )
}