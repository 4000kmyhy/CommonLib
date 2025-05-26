package com.yhy.lib_retrofit.entity

import com.yhy.lib_retrofit.entity.SongData.Album
import com.yhy.lib_retrofit.entity.SongData.Pay
import com.yhy.lib_retrofit.entity.SongData.Singer


/**
 * desc:
 **
 * user: xujj
 * time: 2025/3/24 9:52
 **/
data class SingerSongListData(val singerSongList: Result) {

    data class Result(val data: Data)

    data class Data(
        val singerMid: String,
        val songList: List<SongList>
    )

    data class SongList(val songInfo: SongInfo)

    data class SongInfo(
        val mid: String,
        val title: String,
        val album: Album,
        val singer: List<Singer>,
        val pay: Pay
    ) {
        fun toSongData(): SongData {
            return SongData(
                songmid = mid,
                songname = title,
                albummid = album.mid,
                albumname = album.title,
                singer = singer,
                pay = pay
            )
        }
    }

    fun getSingerMid() = singerSongList.data.singerMid

    fun getSongList(includePay: Boolean = false): List<SongData> {
        val list = ArrayList<SongData>()
        singerSongList.data.songList.forEach {
            it.songInfo.toSongData().let {
                if (includePay || it.isFree()) {
                    list.add(it)
                }
            }
        }
        return list
    }
}