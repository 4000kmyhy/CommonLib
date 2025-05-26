package com.yhy.lib_retrofit.service

import com.yhy.lib_retrofit.entity.MusicKey
import com.yhy.lib_retrofit.entity.SingerListData
import com.yhy.lib_retrofit.entity.SingerSongListData
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * desc:
 **
 * user: xujj
 * time: 2025/3/18 11:51
 **/
interface MusicApiService : ApiService {

    companion object {
        private const val baseUrl = "https://u.y.qq.com/cgi-bin/"

        private fun create(): MusicApiService {
            return ApiService.create(baseUrl)
        }

        suspend fun getMusicUrls(ids: Array<String>): List<String> {
            try {
                val musicKey = create().getMusicKey(data = getKeyData(ids))
                return musicKey.getMusicUrls()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return listOf()
        }

        suspend fun getSingerList(
            area: Int = -100,
            sex: Int = -100,
            genre: Int = -100,
            index: Int = -100,
            cur_page: Int = 0
        ): SingerListData? {
            try {
                val data = getSingerData(area, sex, genre, index, cur_page)
                return create().getSingerList(data = data)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        suspend fun getSingerSongList(
            singerMid: String,
            page: Int,
            order: Int = 1
        ): SingerSongListData? {
            try {
                val data = getSingerSongListData(singerMid, page, order)
                return create().getSingerSongList(data = data)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        private fun getKeyData(ids: Array<String>): String {
            val jsonObject = JSONObject()
            val result = JSONObject()
            result.put("module", "vkey.GetVkeyServer")
            result.put("method", "CgiGetVkey")
            val param = JSONObject()
            param.put("guid", "1")
            param.put("songmid", JSONArray(ids))
//            param.put("uin", "1")
            result.put("param", param)
            jsonObject.put("result", result)
            return jsonObject.toString()
        }

        private fun getSingerData(
            area: Int = -100,//-100:全部、200:内地、2:港台、5:欧美、4:日本、3:韩国、6:其他
            sex: Int = -100,//-100：全部、0：男、1：女、2：组合
            genre: Int = -100,//-100：全部、1：流行、6：嘻哈、2：摇滚、4：电子、3：民谣、8：R&B、10：民歌、9：轻音乐、5：爵士、14：古典、25：乡村、20：蓝调
            index: Int = -100,//-100：热门、1~26：A~Z、27：#
            cur_page: Int = 0
        ): String {
            val jsonObject = JSONObject()
            val result = JSONObject()
            result.put("module", "Music.SingerListServer")
            result.put("method", "get_singer_list")
            val param = JSONObject()
            param.put("area", area)
            param.put("sex", sex)
            param.put("genre", genre)
            param.put("index", index)
            param.put("sin", cur_page * 100)
            param.put("cur_page", cur_page)
            result.put("param", param)
            jsonObject.put("result", result)
            return jsonObject.toString()
        }

        private fun getSingerSongListData(
            singerMid: String,
            page: Int,
            order: Int = 1
        ): String {
            val num = 100
            val jsonObject = JSONObject()
            val result = JSONObject()
            result.put("module", "musichall.song_list_server")
            result.put("method", "GetSingerSongList")
            val param = JSONObject()
            param.put("singerMid", singerMid)
            param.put("begin", page * num)
            param.put("num", num)
            param.put("order", order)
            result.put("param", param)
            jsonObject.put("singerSongList", result)
            return jsonObject.toString()
        }
    }

    @GET("musicu.fcg")
    suspend fun getMusicKey(
        @Query("format") format: String = "json",
        @Query("data") data: String
    ): MusicKey

    @GET("musicu.fcg")
    suspend fun getSingerList(
        @Query("format") format: String = "json",
        @Query("data") data: String
    ): SingerListData

    @GET("musicu.fcg")
    suspend fun getSingerSongList(
        @Query("format") format: String = "json",
        @Query("data") data: String
    ): SingerSongListData
}