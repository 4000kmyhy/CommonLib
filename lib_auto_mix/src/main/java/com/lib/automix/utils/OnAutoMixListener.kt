package com.lib.automix.utils

/**
 * desc:
 **
 * user: xujj
 * time: 2024/7/5 17:41
 **/
interface OnAutoMixListener {

    fun onPlayStateChanged(isDeckA: Boolean, state: Int, playStateChanged: Boolean)

    fun fadeVolume(isFading: Boolean)

    fun onNotifyPlay()
}