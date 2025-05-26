package com.yhy.lib_retrofit.service

import android.util.Log
import com.yhy.lib_retrofit.entity.AlbumInfo
import com.yhy.lib_retrofit.entity.TopListData
import com.yhy.lib_retrofit.entity.TopListDetailData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * desc:
 **
 * user: xujj
 * time: 2025/3/17 17:08
 **/
interface TopListApiService : ApiService {

    companion object {
        //https://c.y.qq.com/v8/fcg-bin/fcg_myqq_toplist.fcg?format=json
        //https://c.y.qq.com/v8/fcg-bin/fcg_v8_toplist_cp.fcg?format=json&topid=4
        //https://c.y.qq.com/v8/fcg-bin/fcg_v8_album_info_cp.fcg?albummid=
        private const val baseUrl = "https://c.y.qq.com/v8/fcg-bin/"

        private fun create(): TopListApiService {
            return ApiService.create(baseUrl)
        }

        suspend fun getTopList(): List<TopListData.TopList> {
            try {
                val topListData = create().getTopListData()
                return topListData.getTopList()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return listOf()
        }

        suspend fun getTopListDetail(topid: Int): TopListDetailData? {
            try {
                return create().getTopListDetailData(topid = topid)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        suspend fun getAlbumInfo(albummid: String): AlbumInfo? {
            try {
                return create().getAlbumInfoData(albummid = albummid)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        suspend fun loadAlbumInfo(albummid: String) {
            val call = create().getAlbumInfoCall(albummid)
            call.enqueue(object : Callback<AlbumInfo>{
                override fun onResponse(p0: Call<AlbumInfo>, p1: Response<AlbumInfo>) {
                    Log.d("xxx", "onResponse: "+p1.body())
                }

                override fun onFailure(p0: Call<AlbumInfo>, p1: Throwable) {
                    Log.d("xxx", "onFailure: "+p1)
                }
            })
        }
    }

    @GET("fcg_myqq_toplist.fcg")
    suspend fun getTopListData(
        @Query("format") format: String = "json"
    ): TopListData

    @GET("fcg_v8_toplist_cp.fcg")
    suspend fun getTopListDetailData(
        @Query("format") format: String = "json",
        @Query("topid") topid: Int
    ): TopListDetailData

    @GET("fcg_v8_album_info_cp.fcg")
    suspend fun getAlbumInfoData(
        @Query("albummid") albummid: String
    ): AlbumInfo

    @GET("fcg_v8_album_info_cp.fcg")
    fun getAlbumInfoCall(
        @Query("albummid") albummid: String
    ): Call<AlbumInfo>
}