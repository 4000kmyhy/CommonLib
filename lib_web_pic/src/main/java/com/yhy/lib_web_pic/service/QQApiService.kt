package com.yhy.lib_web_pic.service

import com.yhy.lib_web_pic.entity.QQData
import com.yhy.lib_web_pic.utils.RetrofitUtils
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * desc:
 **
 * user: xujj
 * time: 2025/4/25 15:24
 **/
interface QQApiService {

    companion object {
        //https://c.y.qq.com/soso/fcgi-bin/client_search_cp?format=json&p=0&n=5&w=
        private const val baseUrl = "https://c.y.qq.com/soso/fcgi-bin/"

        private val mService by lazy {
            RetrofitUtils
                .build(baseUrl)
                .create(QQApiService::class.java)
        }

        suspend fun getQQData(w: String, n: Int = 5): QQData? {
            try {
                return mService.getQQData(w = w, n = n)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }
    }

    @GET("client_search_cp")
    suspend fun getQQData(
        @Query("w") w: String,
        @Query("format") format: String = "json",
        @Query("p") p: Int = 0,
        @Query("n") n: Int = 99
    ): QQData
}