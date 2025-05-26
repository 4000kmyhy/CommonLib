package com.yhy.lib_web_pic.service

import com.yhy.lib_web_pic.entity.KuwoData
import com.yhy.lib_web_pic.utils.RetrofitUtils
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * desc:
 **
 * user: xujj
 * time: 2025/4/25 15:24
 **/
interface KuwoApiService {

    companion object {
        //https://kuwo.cn/search/searchMusicBykeyWord?vipver=1&client=kt&ft=music&cluster=0&strategy=2012&encoding=utf8&rformat=json&mobi=1&issubtitle=1&show_copyright_off=1&pn=0&rn=20&all=
        private const val baseUrl = "https://kuwo.cn/search/"

        private val mService by lazy {
            RetrofitUtils
                .build(baseUrl)
                .create(KuwoApiService::class.java)
        }

        suspend fun getKuwoData(all: String, n: Int = 5): KuwoData? {
            try {
                return mService.getKuwoData(all = all, rn = n)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }
    }

    @GET("searchMusicBykeyWord")
    suspend fun getKuwoData(
        @Query("all") all: String,
        @Query("vipver") vipver: Int = 1,
        @Query("ft") ft: String = "music",
        @Query("encoding") encoding: String = "utf8",
        @Query("mobi") mobi: Int = 1,
        @Query("pn") pn: Int = 0,
        @Query("rn") rn: Int = 5
    ): KuwoData
}