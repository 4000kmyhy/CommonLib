package com.yhy.lib_retrofit.entity

import com.yhy.lib_retrofit.utils.musicUrl

/**
 * desc:
 **
 * user: xujj
 * time: 2025/3/18 15:49
 **/
data class MusicKey(val result: Result) {

    data class Result(val data: Data)

    data class Data(val midurlinfo: List<MidUrlInfo>)

    data class MidUrlInfo(val purl: String)

    fun getMusicUrls(): List<String> {
        val urls = ArrayList<String>()
        result.data.midurlinfo.forEach {
            urls.add(musicUrl + it.purl)
        }
        return urls
    }
}