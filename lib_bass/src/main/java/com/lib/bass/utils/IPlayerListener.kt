package com.lib.bass.utils

import com.lib.bass.BassPlayer

/**
 * desc:
 **
 * user: xujj
 * time: 2023/10/27 11:53
 **/
fun interface IPlayerListener {
    fun onPlayStateChanged(player: BassPlayer, state: Int, playStateChanged: Boolean)
}