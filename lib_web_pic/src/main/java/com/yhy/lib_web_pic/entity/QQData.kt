package com.yhy.lib_web_pic.entity

/**
 * desc:
 **
 * user: xujj
 * time: 2025/4/25 18:18
 **/
data class QQData(val data: Data) {

    data class Data(
        val song: Song
    )

    data class Song(
        val list: List<SongData>
    )

    data class SongData(
        val songname: String,
        val singer: List<Singer>,
        val albummid: String
    )

    data class Singer(
        val name: String
    )

    fun getPicUrl(name: String, artist: String): String {
        data.song.list.forEach { songData ->
            //歌名和歌手都相同
            val isSame = (name.contains(songData.songname, true) ||
                    songData.songname.contains(name, true)) &&
                    isSameSinger(artist, songData.singer)
            //名称包含歌名和歌手
            val isSimilar = name.contains(songData.songname, true) &&
                    isSameSinger(name, songData.singer)
            if (isSame || isSimilar) {
                return getAlbumPicUrl(songData.albummid)
            }
        }
        return ""
    }

    private fun isSameSinger(artist: String, singer: List<Singer>): Boolean {
        singer.forEach {
            if (artist.contains(it.name, true) ||
                it.name.contains(artist, true)
            ) {
                return true
            }
        }
        return false
    }

    companion object {
        fun getAlbumPicUrl(albummid: String, size: Int = 300) =
            "https://y.gtimg.cn/music/photo_new/T002R${size}x${size}M000$albummid.webp"
    }
}