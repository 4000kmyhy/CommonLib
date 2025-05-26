package com.yhy.lib_retrofit.entity

/**
 * desc:
 **
 * user: xujj
 * time: 2025/3/17 14:46
 **/
data class SearchData(val data: Data) {

    data class Data(
        val song: Song
    )

    data class Song(
        val list: List<SongData>
    )

    fun getSongList(includePay: Boolean = false): List<SongData> {
        return data.song.list.filter {
            if (includePay) {
                true
            } else {
                it.isFree()
            }
        }
    }
}



