package com.yhy.lib_retrofit.service

import com.yhy.lib_retrofit.entity.SearchData
import com.yhy.lib_retrofit.entity.SmartBox
import com.yhy.lib_retrofit.entity.SongData
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * desc:
 **
 * user: xujj
 * time: 2025/3/17 16:02
 **/
interface SearchApiService : ApiService {

    companion object {
        //https://c.y.qq.com/soso/fcgi-bin/client_search_cp?format=json&p=0&n=99&w=xxx
        //https://c.y.qq.com/splcloud/fcgi-bin/smartbox_new.fcg?format=json&key=xxx
        private const val baseUrl = "https://c.y.qq.com/soso/fcgi-bin/"
        private const val baseUrl2 = "https://c.y.qq.com/splcloud/fcgi-bin/"

        private fun create(): SearchApiService {
            return ApiService.create(baseUrl)
        }

        private fun create2(): SearchApiService {
            return ApiService.create(baseUrl2)
        }

        suspend fun search(
            p: Int = 0,
            n: Int = 99,
            w: String,
            includePay: Boolean = false,
        ): List<SongData> {
            try {
                val searchData = create().getSearchData(p = p, n = n, w = w)
                return searchData.getSongList(includePay)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return listOf()
        }

        suspend fun smartBox(key: String): SmartBox? {
            try {
                return create2().getSmartBox(key = key)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }
    }

    @GET("client_search_cp")
    suspend fun getSearchData(
        @Query("format") format: String = "json",
        @Query("p") p: Int = 0,
        @Query("n") n: Int = 99,
        @Query("w") w: String
    ): SearchData

    @GET("smartbox_new.fcg")
    suspend fun getSmartBox(
        @Query("format") format: String = "json",
        @Query("key") key: String
    ): SmartBox
}