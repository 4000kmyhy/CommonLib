package com.yhy.lib_web_pic.entity

/**
 * desc:
 **
 * user: xujj
 * time: 2025/4/25 15:34
 **/
data class KuwoData(val abslist: List<Data>) {

    data class Data(
        val web_albumpic_short: String,
        val NAME: String,
        val ARTIST: String
    )

    fun getPicUrl(name: String, artist: String): String {
        abslist.forEach { data ->
            //歌名和歌手都相同
            val isSame = (name.contains(data.NAME, true) ||
                    data.NAME.contains(name, true)) &&
                    isSameSinger(artist, data.ARTIST)
            //名称包含歌名和歌手
            val isSimilar = name.contains(data.NAME, true) &&
                    isSameSinger(name, data.ARTIST)
            if (isSame || isSimilar) {
                return getAlbumPicUrl(data.web_albumpic_short)
            }
        }
        return ""
    }

    private fun isSameSinger(artist: String, singer: String): Boolean {
        val singerArray = singer.split('&')
        singerArray.forEach {
            if (artist.contains(it, true) ||
                it.contains(artist, true)
            ) {
                return true
            }
        }
        return false
    }

    companion object {
        const val albumBaseUrl = "https://img3.kuwo.cn/star/albumcover/"

        fun getAlbumPicUrl(short: String, size: Int = 300): String {
            val index = short.indexOf('/')
            return "$albumBaseUrl$size${short.substring(index)}"
        }
    }
}