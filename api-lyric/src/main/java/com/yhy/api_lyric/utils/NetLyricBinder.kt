package com.yhy.api_lyric.utils

import com.yhy.api_lyric.api.QQMusicApi

/**
 * desc:
 **
 * user: xujj
 * time: 2024/11/25 9:47
 **/
object NetLyricBinder {

    interface OnNetLyricCallback {
        fun onSucceed(lyric: String)

        fun onFailed()
    }

    suspend fun getLyric(
        title: String,
        artist: String,
        callback: OnNetLyricCallback
    ) {
        val searchUrl = QQMusicApi.SearchApi.getUrl("$title $artist")
        OkHttpUtils.get(url = searchUrl, callback = object : OkHttpUtils.OnGetCallback {
            override suspend fun onSucceed(response: String) {
                val songMid = QQMusicApi.SearchApi.getMostSimilarSongId(response, title, artist)
                getLyric(songMid, callback)
            }

            override suspend fun onFailed() {
                callback.onFailed()
            }
        })
    }

    suspend fun getLyric(songMid: String, callback: OnNetLyricCallback) {
        val lyricUrl = QQMusicApi.LyricApi.getUrl(songMid)
        OkHttpUtils.get(
            url = lyricUrl,
            name = QQMusicApi.LyricApi.HeadName,
            value = QQMusicApi.LyricApi.HeadValue,
            callback = object : OkHttpUtils.OnGetCallback {
                override suspend fun onSucceed(response: String) {
                    val lyric = QQMusicApi.LyricApi.getLyric(response)
                    callback.onSucceed(lyric)
                }

                override suspend fun onFailed() {
                    callback.onFailed()
                }
            }
        )
    }

    suspend fun getLyricResults(
        name: String,
        callback: (List<Triple<String, String, String>>?) -> Unit
    ) {
        val searchUrl = QQMusicApi.SearchApi.getUrl(name)
        OkHttpUtils.get(url = searchUrl, callback = object : OkHttpUtils.OnGetCallback {
            override suspend fun onSucceed(response: String) {
                val results = QQMusicApi.SearchApi.getLyricResults(response)
                callback(results)
            }

            override suspend fun onFailed() {
                callback(null)
            }
        })
    }
}