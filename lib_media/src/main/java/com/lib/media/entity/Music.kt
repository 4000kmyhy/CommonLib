package com.lib.media.entity

import android.os.Parcelable
import android.text.TextUtils
import androidx.compose.runtime.Immutable
import com.lib.media.utils.MediaConfig
import kotlinx.parcelize.Parcelize
import java.util.UUID

/**
 * desc:
 **
 * user: xujj
 * time: 2023/4/14 16:49
 **/
@Immutable
@Parcelize
data class Music(
    var id: Long,
    var artistId: Long,
    var albumId: Long,
    @Deprecated("name") var title: String,
    var artist: String,
    var album: String,
    var displayName: String,
    var data: String,
    var duration: Long,
    var dateAdded: Long,
    val uuid: String = UUID.randomUUID().toString()
) : Parcelable {

    companion object{
        fun example(title: String = "xxx") = Music(
            id = -1L,
            artistId = -1L,
            albumId = -1L,
            title = title,
            artist = "",
            album = "",
            displayName = "",
            data = "",
            duration = 0L,
            dateAdded = 0L
        )

        fun Music.new() = this.copy(uuid = UUID.randomUUID().toString())
    }

    /**
     * 获取无后缀文件名
     */
    fun getDisplayNameNoEx(): String {
        if (!TextUtils.isEmpty(displayName)) {
            val dot = displayName.lastIndexOf('.')
            if (dot > -1) {
                return displayName.substring(0, dot)
            }
        }
        return displayName
    }

    fun getExtension(): String {
        if (!TextUtils.isEmpty(displayName)) {
            val dot = displayName.lastIndexOf('.')
            if (dot > -1) {
                return displayName.substring(dot)
            }
        }
        return ""
    }

    val name: String
        get() = if (MediaConfig.getInstance().useDisplayNameOrTitle) {
            getDisplayNameNoEx()
        } else {
            title
        }

    fun rename(
        name: String,
        displayName: String,
        data: String
    ) {
        this.title = name
        this.displayName = displayName
        this.data = data
    }

    override fun toString(): String {
        return "Music(id=$id, title='$title', artist='$artist', album='$album', data='$data', artistId='$artistId', albumId='$albumId')"
    }
}