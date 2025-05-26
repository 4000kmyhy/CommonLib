package com.yhy.lib_retrofit.entity

/**
 * desc:
 **
 * user: xujj
 * time: 2025/3/24 11:45
 **/
data class AlbumInfo(val data: Data) {

    data class Data(
        val color: Int,
        val desc: String,
        val list: List<SongData>,
        val mid: String,
        val name: String,
        val singermid: String,
        val singername: String,
    )
}