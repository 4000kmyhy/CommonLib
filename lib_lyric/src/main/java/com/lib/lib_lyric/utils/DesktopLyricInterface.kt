package com.lib.lib_lyric.utils

import android.app.Activity
import android.content.Context

/**
 * desc:
 **
 * user: xujj
 * time: 2024/11/11 11:55
 **/
interface DesktopLyricInterface {

    fun isPlaying(): Boolean
    fun getCurrentPosition(): Int
    fun getDuration(): Int

    fun playOrPause()
    fun playNext(isNext: Boolean)
    fun onClosed(context: Context)
    fun onLocked(context: Context)

    fun getMainClass(): Class<out Activity>?

    fun getUpdatePlayStateAction(): String
}