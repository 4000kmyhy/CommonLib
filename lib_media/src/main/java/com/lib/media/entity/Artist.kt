package com.lib.media.entity

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize
import java.util.UUID

/**
 * desc:
 **
 * user: xujj
 * time: 2023/4/14 17:14
 **/
@Immutable
@Parcelize
data class Artist(
    var id: Long,
    var name: String,
    var musicCount: Int,
    val uuid: String = UUID.randomUUID().toString()
) : Parcelable