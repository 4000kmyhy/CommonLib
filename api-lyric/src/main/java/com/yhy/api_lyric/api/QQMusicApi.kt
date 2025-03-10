package com.yhy.api_lyric.api

import org.json.JSONException
import org.json.JSONObject

/**
 * desc:
 **
 * user: xujj
 * time: 2024/11/25 9:43
 **/
object QQMusicApi {

    object SearchApi {
        fun getUrl(name: String, page: Int = 1, num: Int = 10) =
            "https://c.y.qq.com/soso/fcgi-bin/client_search_cp?p=$page&n=$num&w=$name" +
                    "&format=json&loginUin=0&hostUin=0&inCharset=utf8&outCharset=utf-8&notice=0&platform=yqq&needNewCode=0&remoteplace=txt.yqq.song&t=0&aggr=1&cr=1&catZhida=1&flag_qc=0"

        fun getMostSimilarSongId(
            response: String,
            title: String,
            artist: String
        ): String {
            try {
                val jsonObject = JSONObject(response)
                val code = jsonObject.optInt("code", -1)
                if (code == 0) {
                    val data = jsonObject.getJSONObject("data")
                    val song = data.getJSONObject("song")
                    val list = song.getJSONArray("list")
                    var songId = ""
                    for (i in 0 until list.length()) {
                        val temp = list.optJSONObject(i)
                        val singer = temp.getJSONArray("singer")
                        var artistName = ""
                        for (j in 0 until singer.length()) {
                            val tempSinger = singer.optJSONObject(j)
                            val name = tempSinger.optString("name")
                            artistName += if (j == 0) {
                                name
                            } else {
                                "/$name"
                            }
                        }
                        val songMid = temp.optString("songmid")
                        val songName = temp.optString("songname")

                        if (songId.isEmpty()) {//先获取第一个
                            songId = songMid
                        }
                        if ((title.contains(songName, true) ||
                                    songName.contains(title, true)) &&
                            (artist.contains(artistName, true) ||
                                    artistName.contains(artist, true))
                        ) {//匹配歌名和歌手
                            songId = songMid
                            break
                        }
                    }
                    return songId
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return ""
        }

        fun getLyricResults(response: String): List<Triple<String, String, String>> {
            val lyricResults = ArrayList<Triple<String, String, String>>()
            try {
                val jsonObject = JSONObject(response)
                val code = jsonObject.optInt("code", -1)
                if (code == 0) {
                    val data = jsonObject.getJSONObject("data")
                    val song = data.getJSONObject("song")
                    val list = song.getJSONArray("list")
                    for (i in 0 until list.length()) {
                        val temp = list.optJSONObject(i)
                        val singer = temp.getJSONArray("singer")
                        var artistName = ""
                        for (j in 0 until singer.length()) {
                            val tempSinger = singer.optJSONObject(j)
                            val name = tempSinger.optString("name")
                            artistName += if (j == 0) {
                                name
                            } else {
                                "/$name"
                            }
                        }
                        val songMid = temp.optString("songmid")
                        val songName = temp.optString("songname")

                        lyricResults.add(Triple(songMid, songName, artistName))
                    }
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return lyricResults
        }
    }

    object LyricApi {
        const val HeadName = "Referer"
        const val HeadValue = "https://y.qq.com/portal/player.html"

        fun getUrl(songmid: String): String {
            return "https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg?format=json&nobase64=1&songmid=$songmid"
        }

        fun getLyric(response: String): String {
            try {
                val jsonObject = JSONObject(response)
                val code = jsonObject.optInt("code", -1)
                if (code == 0) {
                    return jsonObject.optString("lyric")
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
            return ""
        }
    }
}