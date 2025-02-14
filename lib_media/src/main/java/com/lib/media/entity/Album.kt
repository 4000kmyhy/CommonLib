package com.lib.media.entity

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize
import java.util.UUID

/**
 * desc:
 **
 * user: xujj
 * time: 2023/4/14 17:16
 **/
@Immutable
@Parcelize
data class Album(
    var id: Long,
    var name: String,
    var artistId: Long,
    var artistName: String,
    var musicCount: Int,
    val uuid: String = UUID.randomUUID().toString()
) : Parcelable