package com.yhy.lib_retrofit.entity

/**
 * desc:
 **
 * user: xujj
 * time: 2025/3/17 17:20
 **/
data class TopListData(val data: Data) {

    data class Data(
        val topList: List<TopList>
    )

    data class TopList(
        val id: Int,
        val listenCount: Int,
        val picUrl: String,
        val songList: List<SongList>
    )

    data class SongList(
        val singername: String,
        val songname: String
    )

    fun getTopList(): List<TopList> {
        return data.topList
    }
}

data class TopListDetailData(
    val color: Int,
    val date: String,
    val songlist: List<Songlist>,
    val topinfo: Topinfo
) {
    data class Songlist(
        val data: SongData
    )

    data class Topinfo(
        val topID: Int,
        val ListName: String,
        val info: String,
        val listennum: Int,
        val pic: String,
        val pic_v12: String
    )

    fun getSongList(includePay: Boolean = false): List<SongData> {
        val list = ArrayList<SongData>()
        songlist.forEach {
            it.data.let {
                if (includePay || it.isFree()) {
                    list.add(it)
                }
            }
        }
        return list
    }
}