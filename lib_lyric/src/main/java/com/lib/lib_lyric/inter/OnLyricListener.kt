package com.lib.lib_lyric.inter

/**
 * desc:
 **
 * user: xujj
 * time: 2024/2/22 15:53
 **/
abstract class OnLyricListener {
    abstract fun onViewClick(hasLrc: Boolean)
    open fun onPlayClick(time: Long): Boolean {
        return false
    }
}