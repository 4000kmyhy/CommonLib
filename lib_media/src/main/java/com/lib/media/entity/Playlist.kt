package com.lib.media.entity

import android.content.Context
import android.os.Parcelable
import androidx.compose.runtime.Immutable
import com.lib.media.utils.MusicLoader
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.util.UUID

/**
 * desc:
 **
 * user: xujj
 * time: 2023/4/28 9:32
 **/
@Immutable
@Parcelize
data class Playlist(
    var id: Long,
    var name: String,
    var musicCount: Int,
    val uuid: String = UUID.randomUUID().toString()
) : Parcelable {

    @IgnoredOnParcel
    private val musicIdList = ArrayList<Long>()

    fun addMusic(musicId: Long) {
        if (!musicIdList.contains(musicId)) {
            musicIdList.add(musicId)
        }
    }

    fun countMusic(context: Context) {
        val musicList = MusicLoader.getMusicListByIds(context, musicIdList)
        musicCount = musicList.size
    }
}